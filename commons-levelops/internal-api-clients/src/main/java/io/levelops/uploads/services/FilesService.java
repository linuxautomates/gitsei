package io.levelops.uploads.services;

import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.FileUpload;

import java.io.IOException;
import java.util.UUID;

public interface FilesService {

    /**
     * Upload a file to the levelops uploads store associated with a sub component.
     * 
     * @param company tenant
     * @param user the uploading user
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param subComponent the subcomponent type/name associated with the file 
     * @param subComponentId the id of the subcomponent
     * @param fileUpload the file to be uploaded
     * @return the id of the file in the levelops storage
     */
    UUID uploadForSubComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final String subComponent, final String subComponentId, final FileUpload fileUpload) throws IOException;

    /**
     * Uploads a file to the levelops uploads store associated with a component.
     * 
     * @param company tenant
     * @param user the uploading user
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @param fileUpload the file to be uploaded
     * @return the id of the file in the levelops storage
     */
    UUID uploadForComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final FileUpload fileUpload) throws IOException;
    
    /**
     * Retrieves a file associated with a subcomponent.
     * 
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param subComponent the subcomponent type/name associated with the file 
     * @param subComponentId the id of the subcomponent
     * @param fileId the id of the file
     * @return
     */
    FileUpload downloadForSubComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final String subComponent, final String subComponentId, final UUID fileId) throws IOException;

    /**
     * Retrieves a file associated with a component.
     *
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param fileId the id of the file
     * @return
     */
    FileUpload downloadForComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final UUID fileId) throws IOException;
    
    /**
     * Updates an upload associated with a subcomponent.
     *
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param subComponent the subcomponent type/name associated with the file 
     * @param subComponentId the id of the subcomponent
     * @param fileUpload the file to be uploaded
     * @return true only if the file is successfully uploaded
     */
    boolean updateForSubComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final String subComponent, final String subComponentId, final UUID fileId, final FileUpload fileUpload) throws IOException;

    /**
     * Updates an upload associated with a component.
     *
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param fileUpload the file to be uploaded
     * @return true only if the file is successfully uploaded
     */
    boolean updateForComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final UUID fileId, final FileUpload fileUpload) throws IOException;

    /**
     * Deletes a file associated with a subcomponent.
     * 
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param subComponent the subcomponent type/name associated with the file 
     * @param subComponentId the id of the subcomponent
     * @param fileId the id of the file
     * @return
     */
    boolean deleteForSubComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final String subComponent, final String subComponentId, final UUID fileId) throws IOException;

    /**
     * Deletes a file associated with a component.
     *
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param fileId the id of the file
     * @return
     */
    boolean deleteForComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final UUID fileId) throws IOException;
    
    /**
     * Deletes a file associated with a subcomponent.
     * 
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @param subComponent the subcomponent type/name associated with the file 
     * @param subComponentId the id of the subcomponent
     * @return
     */
    boolean deleteEverythingUnderSubComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId, final String subComponent, final String subComponentId) throws IOException;

    /**
     * Deletes a file associated with a component.
     *
     * @param company tenant
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param component the component type associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the component
     * @return
     */
    boolean deleteEverythingUnderComponent(final String company, final String user, final RoleType role, final ComponentType component, final String componentId) throws IOException;

    /**
     * Uploads a file to the path. The path needs to be empty and the user must be authorized to upload content to the parent path.
     * 
     * @param path the full path on the levelops uploads store for the location to host the file.
     * @param user the user uploading the file.
     * @param role the role of the user uploading the file.
     * @param id the id of the upload
     * @param fileUpload the file to be uploaded
     * @return true only if the file is successfully uploaded
     */
    boolean upload(final String path, final String user, final RoleType role, final FileUpload fileUpload) throws IOException;

    /**
     * Download a file from located at the path if the user is authorized to see it.
     * 
     * @param path
     * @param user
     * @param role
     * @return
     */
    FileUpload download(final String path, final String user, final RoleType role) throws IOException;

    /**
     * Updates a file located at the path if the user is authorized to do so.
     * 
     * @param path
     * @param user
     * @param role
     * @param fileId
     * @param fileUpload the file to be uploaded
     * @return true if the file id successfully updated
     */
    boolean update(final String path, final String user, final RoleType role, final FileUpload fileUpload) throws IOException;

    /**
     * Deletes an upload provided the user is authorized to delete it.
     * 
     * @param path the full path on the levelops uploads store for the file.
     * @param user the user attempting to delete the file.
     * @param role the role of the user attempting to delete the file.
     * @return true only if the file has been successfully deleted.
     */
    boolean delete(final String path, final String user, final RoleType role) throws IOException;
}