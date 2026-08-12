# Local PMS policy files (from GET /v1/policymanager/policies)
#
# pms-all-policy.json              — all policies keyed by policyId
# partner-credential-policy-index.json — partnerId|credentialType → policy (used by PolicyUtil)
#
# Enable with: mosip.idrepo.policy.local-source=true (application-local.properties)
#
# WireMock serves the same dump at:
#   GET http://localhost:8082/v1/policymanager/policies
