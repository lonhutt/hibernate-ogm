/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.impl.Immutable;
import org.hibernate.service.Service;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * A service which provides access to state specific to one event cycle (currently (auto)-flush or persist).
 * <p>
 * Client code (such as persisters, dialects etc.) may use this service to propagate state amongst each other, as long
 * as they are within the same event cycle. States are identified by class objects which are used as key when accessing
 * the contextual map. If a given state type is accessed for the first time during an event cycle, its associated
 * {@link EventStateLifecycle} will be invoked to obtain a new instance of that state type.
 * <p>
 * Accessing the context when not being within the scope of a supported event cycle is illegal.
 * <p>
 * The service state is managed by listeners such as {@link EventContextManagingAutoFlushEventListener} which make sure
 * that the context is destroyed upon event cycle completion.
 *
 * @author Gunnar Morling
 */
public class EventContextManager implements Service {

	private final ThreadLocal<Map<Class<?>, Object>> stateHolder;

	@Immutable
	private final Map<Class<?>, EventStateLifecycle<?>> lifecycles;

	public EventContextManager() {
		this.stateHolder = new ThreadLocal<>();
		this.lifecycles = Collections.unmodifiableMap( EventStateLifecycles.getLifecycles() );
	}

	/**
	 * Whether any components will make use of the event context or not.
	 */
	public static boolean isEventContextRequired(Map<Object, Object> settings, SessionFactoryServiceRegistry serviceRegistry) {
		GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
		BatchOperationsDelegator batchDelegator = GridDialects.getDelegateOrNull( gridDialect, BatchOperationsDelegator.class );

		return settings.get( OgmProperties.ERROR_HANDLER ) != null || batchDelegator != null;
	}

	void onEventBegin(EventSource session) {
		Map<Class<?>, Object> stateMap = new HashMap<>();
		stateMap.put( SessionImplementor.class, session );
		stateHolder.set( stateMap );
	}

	void onEventFinished() {
		Map<Class<?>, Object> states = stateHolder.get();
		if ( states == null ) {
			return;
		}

		SessionImplementor session = (SessionImplementor) states.get( SessionImplementor.class );

		for ( Entry<Class<?>, Object> state : states.entrySet() ) {
			if ( state.getValue() != session ) {
				onFinish( state.getKey(), state.getValue(), session );
			}
		}

		stateHolder.remove();
	}

	private <T> void onFinish(Class<T> stateType, Object state, SessionImplementor session) {
		@SuppressWarnings("unchecked")
		T typedState = (T) state;

		getLifecycle( stateType ).onFinish( typedState, session );
	}

	/**
	 * Returns the state object of the given type.
	 * <p>
	 * <b>Note:</b> Must only be called when being within a supported event cycle.
	 */
	public <T> T get(Class<T> stateType) {
		Map<Class<?>, Object> states = getStates();
		T value = getState( states, stateType );

		if ( value == null ) {
			value = create( stateType, states );
			states.put( stateType, value );
		}

		return value;
	}

	/**
	 * Whether the event context currently is active (i.e. we are within a supported event cycle such as flush, persist)
	 * or not.
	 */
	public boolean isActive() {
		return stateHolder.get() != null;
	}

	private <T> T create(Class<T> stateType, Map<Class<?>, Object> states) {
		EventStateLifecycle<T> lifeycle = getLifecycle( stateType );

		if ( lifeycle == null ) {
			throw new IllegalStateException( "No lifecycle found for state type: " + stateType );
		}

		SessionImplementor session = getState( states, SessionImplementor.class );
		return lifeycle.create( session );
	}

	private Map<Class<?>, Object> getStates() {
		Map<Class<?>, Object> states = stateHolder.get();

		if ( states == null ) {
			throw new IllegalStateException( "Must not access event cycle state if not within a supported event cycle " + "(flush, auto-flush, persist)" );
		}

		return states;
	}

	private <T> T getState(Map<Class<?>, Object> states, Class<T> stateType) {
		@SuppressWarnings("unchecked")
		T value = (T) states.get( stateType );
		return value;
	}

	private <T> EventStateLifecycle<T> getLifecycle(Class<T> stateType) {
		@SuppressWarnings("unchecked")
		EventStateLifecycle<T> lifecycle = (EventStateLifecycle<T>) lifecycles.get( stateType );
		return lifecycle;
	}
}
