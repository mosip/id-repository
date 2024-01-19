## What is Handle?

Handle is any unique piece of data possessed by the resident. It can be a unique username created by the resident. 
Other possible handles could be phone number, email address, or any linked functional/sectoral IDs. 

Handles are IDs that are frequently used by a resident and easy to remember piece of data.

## Principles

* Resident could have more than one handle mapped to his/her UIN.
* Handles should be case-insensitive.
* The usage of handles should be driven through policy.
* Resident should be given the freedom to choose which handles to use.

## Create Handle

* Mark a field to be a handle in Identity schema.
* Introduced reserved field "selectedHandles" that holds the list of resident selected handle fieldIds.
* Table "mosip_idrepo.handle" will hold hash of the handle linked with UIN.
* Changes in add_identity API:
    1. Identify if any handle is selected in the input.
    2. Check if the selected handles are configured to be a HANDLE in identity_schema(JSON schema validation).
    3. If no handles selected, proceeds with step 7.
    4. Get the salt for the input handle and generate selected handle salted hash.
    5. Check if an entry exists with the same handle_hash in the mosip_idrepo.handle table.
    6. Fails the add_identity request if entry exists.
    7. Otherwise, create an identity with UIN and Create an entry in the handle table for each selected handle.
    8. Issue credential with the UIN. As part of handle support, we have made this issuance configurable.
    9. Issue credential with the handle for each selected handles.
* There were changes in IDA to support handle as new IDType and also introduced regex based handle validation.
* Changes in update_identity API: (Not implemented)


## Questions

1. If the system is configured to use more than one functional ID as a handle. Collision between 2 different functional 
IDs results in denying the creation of a handle for a resident.
Solution: Need store handle along with handle-type, On every authenticate request system will expect handle and handle_type as input,
or handle_type should be prefixed to the handle.

It is decided to append configured postfix to the input handle.
Example: 
Input is phone number -> +9134523233 and the configured postfix is @phone, then the final handle stored in ID-repo is
+9134523233@phone. Now during authentication, resident should use "+9134523233@phone" as individualId. Authentication 
portals will support automatic appending of handle-type postfix or could provide option to the resident to choose among 
the list of postfixes(makes sense when more than one handles are supported).

2. Custom user handle can be any unique username chosen/created by resident. Is it required to store this in ID-repo?
Solution: This should be unique and should be linked to resident UIN/VID. eSignet itself can have a feature to create a 
user handle and store the user handle mapped to the verified VID/UIN. When the resident tries to authenticate with the 
handle, eSignet finds the mapping and requests the IDA with the mapped VID/UIN. 
