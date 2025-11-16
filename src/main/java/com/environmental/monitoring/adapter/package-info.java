/**
 * Adapter module containing technical implementations of the hexagonal architecture.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>Inbound adapters (driving adapters) - Web/OData layer</li>
 *   <li>Outbound adapters (driven adapters) - Persistence layer</li>
 * </ul>
 *
 * <p>This module depends on both domain and application modules to implement
 * the ports defined in the application layer.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Infrastructure Adapters",
    allowedDependencies = {"domain", "application"}
)
package com.environmental.monitoring.adapter;
