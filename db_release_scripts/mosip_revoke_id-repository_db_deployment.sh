### -- ---------------------------------------------------------------------------------------------------------
### -- Script Name		: Revoke DB deploy
### -- Deploy Module 	: MOSIP ID Repository
### -- Purpose    		: To revoke MOSIP Database alter scripts for the release, Scripts revoke changes for id repository databases.      
### -- Create By   		: Sadanandegowda
### -- Created Date		: 07-Jan-2020
### -- 
### -- Modified Date        Modified By         Comments / Remarks
### -- -----------------------------------------------------------------------------------------------------------

### -- -----------------------------------------------------------------------------------------------------------

#! bin/bash
echo "`date` : You logged on to DB deplyment server as : `whoami`"
echo "`date` : MOSIP Database objects release deployment revoke started.... Release Number : $1"

echo "=============================================================================================================="
bash ./mosip_idrepo/idrepo_revoke_db_deploy.sh ./mosip_idrepo/idrepo_release_deploy.properties $1
echo "=============================================================================================================="

echo "=============================================================================================================="
bash ./mosip_idmap/idmap_revoke_db_deploy.sh ./mosip_idmap/idmap_release_deploy.properties $1
echo "=============================================================================================================="

echo "=============================================================================================================="
bash ./mosip_credential/credential_revoke_db_deploy.sh ./mosip_credential/credential_release_deploy.properties $1
echo "=============================================================================================================="

echo "`date` : MOSIP DB Release Deployment revoke for id repository databases is completed, Please check the logs at respective logs directory for more information"
 
