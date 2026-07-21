package io.mosip.idrepository.core.jobs;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.mosip.idrepository.core.manager.partner.PartnerCacheUpdateSchedulerConfig;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;

/**
 * Spring configuration hub that wires non-pipeline scheduled jobs for ID Repository core.
 *
 * <p>
 * Historically this package aggregated several {@code @Scheduled} jobs (credential status
 * polling, batch reprocess, etc.). Credential issuance now runs synchronously in-process
 * in the consolidated service. What remains here is a thin {@link Import} of partner
 * Online Verification (OLV) cache eviction.
 * </p>
 *
 * <h2>What this class does</h2>
 * <p>
 * {@link Import}s {@link PartnerCacheUpdateSchedulerConfig}, which periodically clears
 * the OLV partner cache used by {@link PartnerServiceManager}. Importing this
 * {@code @Configuration} from identity / library config ensures the partner cache
 * refresh bean is registered without duplicating scheduler definitions.
 * </p>
 *
 * <h2>What is not here</h2>
 * <ul>
 *   <li>Credential status — service {@code CredentialStatusManager} (synchronous after identity save)</li>
 *   <li>Credential issuance — in-process {@code CredentialIssuanceProcessor} (no Spring Batch)</li>
 *   <li>Salt population — {@code id-repository-salt-generator} Kubernetes Job only</li>
 *   <li>{@code id_attributes} cache eviction — {@code IdRepoSecurityManager}</li>
 * </ul>
 *
 * <h2>Wiring</h2>
 * <pre>
 * // typically imported from identity / IdRepo library config
 * {@literal @}Import(IdRepoSchedulerConfiguration.class)
 * </pre>
 * <p>
 * Once imported, {@link PartnerCacheUpdateSchedulerConfig#clearPartnerDataCache()} runs
 * on {@code mosip.idrepo.cache.update.interval}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Identity / library Spring configuration that needs partner cache refresh</li>
 *   <li>Indirectly: credential issuance and WebSub publish paths that read OLV partners
 *       via {@link PartnerServiceManager}</li>
 * </ul>
 *
 * @see PartnerCacheUpdateSchedulerConfig
 * @see PartnerServiceManager
 */
@Configuration
@Import(PartnerCacheUpdateSchedulerConfig.class)
public class IdRepoSchedulerConfiguration {

}
