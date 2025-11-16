/**
 * Domain module containing core business entities and value objects.
 * This is the center of the hexagonal architecture.
 *
 * <p>This module is shared across all other modules as it represents
 * the core domain model of the environmental monitoring system.</p>
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    displayName = "Domain Model"
)
package com.environmental.monitoring.domain;
