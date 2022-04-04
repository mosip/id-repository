-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: May-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Jul-2021		Ram Bhatt	    Creation of uin biometric draft and uin draft tables
-- Jul-2021		Ram Bhatt	    Lang Code nullable for multiple tables
-- Jul-2021		Manoj SP	    Addition of anonymous profile column to tables
-- Sep-2021		Manoj SP	    Removed Anonymous Profile alter scripts
-- Sep-2021		Manoj SP	    Added anonymous_profile and channel_info tables.
-------------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------