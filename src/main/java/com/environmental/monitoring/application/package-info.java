/**
 * Application module containing use cases, ports, and service implementations.
 * This module orchestrates the domain model and defines the boundaries
 * for the hexagonal architecture.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>Input ports (driving ports) - interfaces for use cases</li>
 *   <li>Output ports (driven ports) - interfaces for infrastructure</li>
 *   <li>Service implementations of input ports</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Application Services",
    allowedDependencies = {"domain"}
)
package com.environmental.monitoring.application;
