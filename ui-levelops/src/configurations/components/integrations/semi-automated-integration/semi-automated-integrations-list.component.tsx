import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Form, Modal, notification } from "antd";
import "./semi-automated-integrations.styles.scss";
import { SelectRestapi } from "shared-resources/helpers";
import { RestTags } from "classes/RestTags";
import { SemiAutomatedIntegrationCard } from "../../integration-card";
import Loader from "components/Loader/Loader";
import { CSV_TOOL } from "../../integration-card/helper";
import {
  pluginsCSVUpload,
  pluginsList,
  pluginsTrigger,
  pluginsUpload,
  productsList,
  restapiClear,
  tagsGetOrCreate,
  tagsList
} from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { WORKSPACES } from "dashboard/constants/applications/names";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { AntSelect } from "shared-resources/components";
import { getSettingsPage } from "constants/routePaths";

interface SemiAutomatedIntegrationsListComponentProps {
  history: any;
  disabled?: boolean;
}
export const SemiAutomatedIntegrationsListComponent: React.FC<SemiAutomatedIntegrationsListComponentProps> = (
  props: SemiAutomatedIntegrationsListComponentProps
) => {
  const dispatch = useDispatch();
  const [plugins, setPlugins] = useState<any>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [upload_loading, setUploadLoading] = useState<boolean>(false);
  const [upload_id, setUploadId] = useState<any>(undefined);
  const [file_name, setFileName] = useState<any>(undefined);
  const [trigger_loading, setTriggerLoading] = useState<boolean>(false);
  const [showUploadModal, setShowUploadModal] = useState<boolean>(false);
  const [products, setProducts] = useState<any>([]);
  const [tags, setTags] = useState<any>([]);
  const [create_tags_loading, setCreateTagsLoading] = useState<boolean>(false);
  const [csv_uploading, setCsvUploading] = useState<boolean>(false);
  const [file, setFile] = useState<any>(null);
  const [plugin, setPlugin] = useState<any>(null);
  const pluginsState = useSelector(state => get(state, ["restapiReducer", "plugins"], {}));
  const tagsState = useSelector(state => get(state, ["restapiReducer", "tags"], {}));
  const pluginsCSVState = useSelector(state => get(state, ["restapiReducer", "plugins_csv"], {}));

  useEffect(() => {
    setLoading(true);
    dispatch(pluginsList());
    return () => {
      dispatch(restapiClear("plugins", "list", "0"));
      dispatch(restapiClear("plugins", "upload", "-1"));
      dispatch(restapiClear("plugins", "trigger", "-1"));
      dispatch(restapiClear("tags", "getOrCreate", "-1"));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const { loading: workspaceLoading, workSpaceListData } = useWorkSpaceList();

  useEffect(() => {
    if (loading) {
      const { loading: apiLoading, error } = get(pluginsState, ["list", "0"], { loading: true, error: true });
      if (!apiLoading) {
        if (error) {
          setLoading(false);
        }
        setLoading(false);
        setPlugins(pluginsState.list[0].data?.records);
      }
    }

    if (!create_tags_loading && upload_loading) {
      const { loading, error } = get(pluginsState, ["upload", upload_id], { loading: true, error: true });
      const tagList = tags;
      if (!loading && error) {
        setUploadLoading(false);
        setUploadId(undefined);
      }
      if (!loading) {
        const data = {
          product_ids: products,
          tag_ids: tagList.map((t: any) => t.key),
          trigger: {
            type: "file",
            value: pluginsState.upload[upload_id].data?.id
          }
        };
        dispatch(pluginsTrigger(upload_id, data));
        notification.success({
          message: "File Upload",
          description: `Uploaded ${file_name} successfully`
        });
        setUploadLoading(false);
        setTriggerLoading(true);
        setFileName(undefined);
        setFile(null);
        setPlugin(null);
      }
    }

    if (trigger_loading) {
      const { loading } = get(pluginsState, ["trigger", upload_id], { loading: true });
      if (!loading) {
        setTriggerLoading(false);
        setUploadId(undefined);
      }
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pluginsState]);

  useEffect(() => {
    if (create_tags_loading) {
      const { loading, error } = get(tagsState, ["getOrCreate", 0], { loading: true, error: true });
      if (!loading && !error) {
        const { existingTags } = RestTags.getNewAndExistingTags(tags);
        const newtags = tagsState.getOrCreate[0].data;
        setTags(
          newtags
            .map((t: any) => ({
              key: t.id,
              label: t.name
            }))
            .concat(existingTags)
        );
        setCreateTagsLoading(false);
        setUploadLoading(true);
      }
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tagsState]);

  useEffect(() => {
    if (!create_tags_loading && csv_uploading) {
      const { loading, error } = get(pluginsCSVState, ["upload", "0"], { loading: true, error: true });
      if (!loading && !error) {
        notification.success({
          message: "File Upload",
          description: `Uploaded ${file_name} successfully`
        });
        setFileName(undefined);
        setCsvUploading(false);
      }
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pluginsCSVState]);

  const handleViewResult = (tool: any) => {
    props.history.push(`${getSettingsPage()}/plugin-results?tab=plugins&ids=${tool.id}&page=1&name=${tool.name}`);
  };

  useEffect(() => {
    if (file === null || plugin === null) {
      return;
    } else {
      showUploadModel();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [file, plugin]);

  const handleFileUpload = () => {
    const { newTags } = RestTags.getNewAndExistingTags(tags);
    if (newTags.length > 0) {
      const tagsToCreate = newTags.map(t => t.key);
      setCreateTagsLoading(true);
      dispatch(tagsGetOrCreate(tagsToCreate));
    }

    const fileName = file?.name;

    if (plugin?.tool === CSV_TOOL) {
      setCsvUploading(true);
      setFileName(fileName);
      const tagList = tags.map((tag: any) => tag.label);
      dispatch(pluginsCSVUpload(file, { tags: tagList, product_ids: products }));
    } else {
      setUploadId(plugin?.id);
      setFileName(fileName);
      dispatch(pluginsUpload(plugin?.id, file));
      setUploadLoading(true);
    }
  };

  const hideUploadModel = () => {
    setShowUploadModal(false);
  };

  const showUploadModel = () => {
    setProducts([]);
    setTags([]);
    setShowUploadModal(true);
    dispatch(restapiClear("tags", "getOrCreate", "-1"));
  };

  const hasSelectedProducts = () => {
    return products.length > 0;
  };

  const workspaceOptions = useMemo(
    () => (workSpaceListData ?? []).map(workspace => ({ label: workspace.name, value: workspace.id })),
    [workSpaceListData]
  );

  const uploadModal = () => {
    if (!showUploadModal) {
      return null;
    }
    return (
      <Modal
        title="Choose Project and Tags"
        visible={showUploadModal}
        onOk={() => {
          hideUploadModel();
          handleFileUpload();
        }}
        onCancel={hideUploadModel}
        cancelText="Cancel"
        okText="Upload"
        closable
        okButtonProps={{ disabled: !hasSelectedProducts() }}>
        <Form layout="vertical">
          <Form.Item label={WORKSPACES} colon={false}>
            <AntSelect
              mode="multiple"
              showSearch
              filterOption
              allowClear
              showArrow
              placeholder="Choose Project..."
              loading={workspaceLoading}
              value={products}
              options={workspaceOptions}
              onChange={(value: any) => {
                setProducts(value);
              }}
            />
          </Form.Item>
          <Form.Item label="Tags" colon={false}>
            <SelectRestapi
              placeholder="Choose Tags..."
              uri="tags"
              fetchData={tagsList}
              searchField="name"
              value={tags}
              allowClear={false}
              mode="multiple"
              createOption={true}
              onChange={(value: any) => {
                setTags(value);
              }}
            />
          </Form.Item>
        </Form>
      </Modal>
    );
  };

  const renderPlugins = () => {
    const pluginsList = plugins || [];
    return pluginsList.map((mapplugin: any, index: any) => (
      <SemiAutomatedIntegrationCard
        key={index}
        integration={mapplugin}
        upload_disabled={upload_loading || trigger_loading}
        onViewResultsClick={(item: any) => handleViewResult(item)}
        onFileUpload={(item: any, uploadFile: any) => {
          setFile(uploadFile);
          setPlugin(item);
        }}
        disabled={props.disabled}
      />
    ));
  };

  if (loading) {
    return <Loader />;
  }
  return (
    <>
      <div className={"integration-cards-grid"}>{renderPlugins()}</div>
      {uploadModal()}
    </>
  );
};

export default SemiAutomatedIntegrationsListComponent;
