### -- ---------------------------------------------------------------------------------------------------------
### -- Script Name		: MOSIP ALL DB Artifacts deployment for ID Repository Module
### -- Deploy Module 	: MOSIP ID Repository Module
### -- Purpose    		: To deploy MOSIP ID Repository Module Database DB Artifacts.       
### -- Create By   		: Sadanandegowda DM
### -- Created Date		: 07-Jan-2020
### -- 
### -- Modified Date        Modified By         Comments / Remarks
### -- -----------------------------------------------------------------------------------------------------------

#! bin/bash
echo "`date` : You logged on to DB deplyment server as : `whoami`"
echo "`date` : MOSIP Database objects deployment started...."

echo "=============================================================================================================="
bash ./mosip_idrepo/mosip_idrepo_db_deploy.sh ./mosip_idrepo/mosip_idrepo_deploy.properties
echo "=============================================================================================================="

echo "=============================================================================================================="
bash ./mosip_idmap/mosip_idmap_db_deploy.sh ./mosip_idmap/mosip_idmap_deploy.properties
echo "=============================================================================================================="

echo "`date` : MOSIP DB Deployment for core databases is completed, Please check the logs at respective logs directory for more information"
 
