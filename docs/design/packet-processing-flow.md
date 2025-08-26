# Packet Processing Flow
## Overview
This document outlines the **end-to-end packet processing flow** highlighting the interactions between the Registration Client, Registration Processor, Identity Repository (IDRepo) and ID Authentication (IDA) modules.
## Sequence Diagram

```mermaid
sequenceDiagram
    participant reg_client as Registration Client
    participant registration_processor as Registration Processor
    participant packet_manager as Packet Manager
    participant identity_service as Identity Service
    participant credential_request_generator as Credential Request Generator
    participant credential_service as Credential Service
    participant data_share as Data Share
    participant websub as WebSub
    participant ida as ID Authentication

    %% Sync and packet upload
    Note over reg_client,registration_processor: STEP 1: Sync and Upload Packets
    reg_client->>registration_processor: Initiate packet metadata sync
    activate registration_processor
    registration_processor-->>reg_client: Packet metadata sync successfully
    deactivate registration_processor

    reg_client->>registration_processor: Upload registration packet
    activate registration_processor
    registration_processor-->>reg_client: Packet uploaded successfully
    deactivate registration_processor

    %% Packet Processing Phase
    Note over registration_processor: STEP 2: Packet Processing
    registration_processor->>packet_manager: Request packet
    activate packet_manager
    packet_manager-->>registration_processor: Receive packet
    registration_processor-->>packet_manager: Connects on need basis
    deactivate packet_manager

    registration_processor->>registration_processor: Process packet
    activate registration_processor

    registration_processor->>identity_service: Store data in IDREPO module
    deactivate registration_processor

    %% Credential Generation Phase
    Note over identity_service: STEP 3: Credential Generation
    activate identity_service
    identity_service->>identity_service: Creates entry with status NEW<br/>into credential_request_status table
    deactivate identity_service

    identity_service->>identity_service: CredentialStatusHandler job runs

    identity_service->>credential_request_generator: Credential generation request for each<br/>UIN/VID/handle (/credentialrequest API)<br/>for partners
    activate credential_request_generator
    credential_request_generator->>credential_request_generator: Entry created with status_code NEW<br/>in credential_transaction table
    credential_request_generator-->>identity_service: UIN/VID/handle credential generated successfully
    deactivate credential_request_generator

    identity_service->>identity_service: Status changed to REQUESTED/FAILED<br/>in credential_request_status table for partner

    %% Credential Issue Phase
    Note over credential_request_generator: STEP 4: Credential Issuance
    credential_request_generator->>credential_request_generator: CredentialProcess job runs
    activate credential_request_generator

    credential_request_generator->>credential_service: Credential issue request (/issue API)
    activate credential_service

    credential_service->>data_share: Request datashare URL<br/>(Biometric and Demographic data)
    activate data_share
    data_share-->>credential_service: Datashare URL generated
    deactivate data_share

    credential_service->>websub: Publish event to IDA
    activate websub
    websub-->>credential_service: Event published successfully
    deactivate websub

    credential_service-->>credential_request_generator: Credential issued successfully
    deactivate credential_service

    credential_request_generator->>credential_request_generator: Change status_code to ISSUED<br/>in credential_transaction table
    deactivate credential_request_generator

    %% IDA Event Processing
    Note over ida: STEP 5: Credential Processing (IDA)
    ida-->>websub: Subscribes to UIN events
    activate websub
    websub-->>ida: Notifies UIN events
    deactivate websub

    ida->>ida: Stores events in credential_event_store table<br/>with status_code NEW
    ida->>ida: CredentialStoreJob runs

    ida->>data_share: Download the data (Demographic and biometrics)<br/>using datashare URL
    activate data_share
    data_share-->>ida: Data downloaded successfully
    deactivate data_share

    ida->>ida: Stores data in identity_cache table<br/>change status_code to STORED in credential_event_store table
    
    %% Credential processing completed IDRepo
    Note over ida: STEP 6: Credential Processing Acknowledgement
    ida->>websub: Send CREDENTIAL_STATUS_UPDATE event    
    credential_request_generator-->>websub: Subscribes to callback events
    activate websub
    websub-->>credential_request_generator: Notifies callback event
    deactivate websub

    credential_request_generator->>credential_request_generator: Change status_code to STORED/FAILED<br/>in credential_transaction table
