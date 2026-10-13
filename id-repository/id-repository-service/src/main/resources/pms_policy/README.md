# Local PMS policy files (from GET /v1/policymanager/policies)
#
# pms-all-policy.json              — all policies keyed by policyId
# partner-credential-policy-index.json — partnerId|credentialType → policy (used by PolicyUtil)
# partner-bioextractors.json       — partnerId|policyId → PMS bioextractors (face/iris/finger)
#                                    plus a "default" fallback. Required when shareableAttributes
#                                    include CBEFF format=extraction.
#
# Enable with: mosip.idrepo.policy.local-source=true (application-local.properties)
#
# WireMock serves the same dump at:
#   GET http://localhost:8082/v1/policymanager/policies
#   GET http://localhost:8082/v1/partnermanager/partners/{partnerId}/bioextractors/{policyId}
