import { notification } from "antd";
import { RestTags } from "classes/RestTags";
import Loader from "components/Loader/Loader";
import { forEach, get } from "lodash";
import queryString from "query-string";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { formClear } from "reduxConfigs/actions/formActions";
import {
  pluginResultsGet,
  pluginResultsUpdate,
  productsGet,
  restapiClear,
  tagsCreate
} from "reduxConfigs/actions/restapi";
import { getIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import { workSpaceGet } from "reduxConfigs/actions/workspaceActions";
import { getformState } from "reduxConfigs/selectors/formSelector";
import {
  getGenericMethodSelector,
  getGenericRestAPISelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";
import { getGenericWorkSpaceMethodSelector } from "reduxConfigs/selectors/workspace/workspace.selector";
import { AntForm, AntFormItem, AntModal } from "shared-resources/components";
import { SelectRestApiNew } from "shared-resources/helpers";
import { getNameWithUUID } from "utils/commonUtils";

import { PluginResultsDetailsComponent, ReportDetailsComponent } from "../pages";

const PRODUCT_NAME = "UNKNOWN";

const ResultsDetailContainer: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [productLoading, setProductLoading] = useState<boolean>(false);
  const resultId = useRef<string | undefined>();
  const [productName, setProductName] = useState<string>(PRODUCT_NAME);
  const [productIds, setProductIds] = useState<any[]>([]);
  const [selectedTags, setSelectedTags] = useState<any[]>([]);
  const [showEditTagsModal, setShowEditTagsModal] = useState<boolean>(false);
  const [tagIds, setTagIds] = useState<any>({});
  const formName = useRef<string | undefined>();
  const [creatingTags, setCreatingTags] = useState<boolean>(false);
  const [createTags, setCreateTags] = useState<any[]>([]);
  const [updatingPluginResults, setUpdatingPluginResults] = useState<boolean>(false);
  const [tagsLoading, setTagsLoading] = useState<boolean>(false);
  const [type, setType] = useState<"report" | "result" | undefined>();
  const dispatch = useDispatch();
  const tagLatestInitialState = useRef<any>(undefined);
  const pluginResultsState = useParamSelector(getGenericRestAPISelector, {
    uri: "plugin_results",
    method: "get",
    uuid: resultId.current
  });

  const pluginResultsUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: "plugin_results",
    method: "update",
    uuid: resultId.current
  });

  const productState = useParamSelector(getGenericWorkSpaceMethodSelector, {
    method: "get"
  });

  const tagCreateState = useParamSelector(getGenericMethodSelector, {
    uri: "tags",
    method: "create"
  });

  const formState = useSelector((state: any) => getformState(state, formName.current));

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    const type = values?.report ? "report" : "result";
    const id = get(values, [type], undefined);
    setType(type);
    resultId.current = id as string;
    setLoading(id !== undefined);
    dispatch(pluginResultsGet(id));
    return () => {
      dispatch(restapiClear("plugin_results", "get", "-1"));
      dispatch(restapiClear("products", "get", "-1"));
    };
  }, []);

  useEffect(() => {
    return () => {
      if (formName.current?.length) {
        dispatch(formClear(formName.current));
      }
    };
  }, [formName]);

  useEffect(() => {
    if (loading) {
      const _loading = get(pluginResultsState, ["loading"], true);
      const error = get(pluginResultsState, ["error"], false);
      if (!_loading) {
        if (!error) {
          const data = get(pluginResultsState, ["data"], {});
          const productIds = get(data, "product_ids", []);
          forEach(productIds, (id: any) => dispatch(workSpaceGet(id)));
          const ids = {
            tag_ids: data?.tags
          };
          const name = getNameWithUUID("tags_filters_map");
          formName.current = name;
          dispatch(getIdsMap(name, ids));
          setProductLoading(productIds.length > 0);
          setTagsLoading(true);
          setProductIds(productIds);
          setTagIds(ids);
        }
        setLoading(false);
      }
    }
  }, [pluginResultsState]);

  useEffect(() => {
    if (tagsLoading) {
      if (Object.keys(tagIds).length === Object.keys(formState).length) {
        let tags = [];
        if (formState?.tag_ids?.length > 0) {
          tags = formState?.tag_ids.map((tag: any) => ({ key: tag.id, label: tag.name }));
        }
        setSelectedTags(tags);
        setTagsLoading(false);
      }
    }
  }, [formState]);

  useEffect(() => {
    if (productLoading) {
      let loading = false;
      let productNames: any[] = [];
      forEach(productIds, (id: any) => {
        const proLoading = get(productState, [id, "loading"], true);
        const proError = get(productState, [id, "error"], false);
        if (!proLoading && !proError) {
          const data = get(productState, [id, "data"], {});
          productNames.push(data?.name);
        } else {
          if (proLoading) {
            loading = true;
          }
        }
      });
      setProductLoading(loading);
      setProductName(loading ? PRODUCT_NAME : productNames.join(", "));
    }
  }, [productState]);

  useEffect(() => {
    if (creatingTags) {
      let createTagsLoading = false;
      let newlyCreatedTags: { key: any; label: any }[] = [];

      createTags.forEach((tag: any) => {
        const tagLoading = get(tagCreateState, [tag?.key, "loading"], true);
        const tagError = get(tagCreateState, [tag?.key, "error"], false);
        if (tagLoading) {
          createTagsLoading = true;
        } else if (!tagError && !tagLoading) {
          const data = get(tagCreateState, [tag?.key, "data"], {});
          newlyCreatedTags.push({
            key: data?.id,
            label: tag?.label
          });
        }
      });

      if (!createTagsLoading) {
        const { existingTags } = RestTags.getNewAndExistingTags(selectedTags);
        const tags = [...existingTags, ...newlyCreatedTags];
        dispatch(
          pluginResultsUpdate(resultId.current, {
            tags: tags.map(tag => tag.label)
          })
        );
        setCreatingTags(false);
        setUpdatingPluginResults(true);
        notification.info({ message: "Updating plugin result..." });
      }
    }
  }, [tagCreateState]);

  useEffect(() => {
    if (updatingPluginResults) {
      const loading = get(pluginResultsUpdateState, ["loading"], true);
      if (!loading) {
        dispatch(restapiClear("plugin_results", "update", "-1"));
        setUpdatingPluginResults(false);
        notification.success({ message: "Plugin Result Updated successfully" });
      }
    }
  }, [pluginResultsUpdateState]);

  const handleTagsChange = useCallback((tags: any) => setSelectedTags(tags), []);

  const handleTagsSave = () => {
    const createTags = RestTags.getNewAndExistingTags(selectedTags)?.newTags;

    if (createTags.length > 0) {
      setCreatingTags(true);
      setCreateTags(createTags);
      console.log("[createTags]", createTags);
      createTags.forEach(tag => {
        let newTag = new RestTags();
        newTag.name = tag?.key?.replace("create:", "");
        dispatch(tagsCreate(newTag, tag.key));
      });
      notification.info({ message: "Creating new Tags..." });
      setShowEditTagsModal(prev => !prev);
      return;
    }
    tagLatestInitialState.current = selectedTags;
    dispatch(
      pluginResultsUpdate(resultId.current, {
        tags: selectedTags.map(tag => tag.label)
      })
    );
    notification.info({ message: "Updating plugin result..." });
    setUpdatingPluginResults(true);
    setShowEditTagsModal(prev => !prev);
  };

  const handleModalClose = useCallback(() => {
    let tags = [];
    if (tagLatestInitialState.current) {
      tags = tagLatestInitialState.current;
    } else if (formState?.tag_ids?.length > 0) {
      tags = (formState?.tag_ids || []).map((tag: any) => ({ key: tag.id, label: tag.name }));
    }
    setSelectedTags(tags);
    setShowEditTagsModal(prev => !prev);
  }, [formState, tagLatestInitialState.current]);

  if (loading) return <Loader />;

  return (
    <>
      {showEditTagsModal && (
        <AntModal visible title="Edit tags" onOk={handleTagsSave} onCancel={handleModalClose} okText="Save" closable>
          <AntForm layout="vertical">
            <AntFormItem label="Tags" colon={false}>
              <SelectRestApiNew
                placeholder="Choose Tags..."
                uri="tags"
                searchField="name"
                value={selectedTags}
                allowClear={false}
                mode="multiple"
                createOption
                onChange={handleTagsChange}
              />
            </AntFormItem>
          </AntForm>
        </AntModal>
      )}
      {type === "report" ? (
        <ReportDetailsComponent
          pluginResultsState={pluginResultsState}
          onEditTagClick={() => setShowEditTagsModal(prev => !prev)}
          productName={productName}
          selectedTags={selectedTags}
        />
      ) : (
        <PluginResultsDetailsComponent
          pluginResultsState={pluginResultsState}
          onEditTagClick={() => setShowEditTagsModal(prev => !prev)}
          productName={productName}
          selectedTags={selectedTags}
        />
      )}
    </>
  );
};

export default ResultsDetailContainer;
