import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { get } from "lodash";
import { AntButton, AntModal } from "shared-resources/components";
import ConfigureAttributes from "../ConfigureAttributes/ConfigureAttributes";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ORG_USER_SCHEMA_ID, orgUsersGenericSelector } from "reduxConfigs/selectors/orgUsersSelector";
import { OrgUserSchemaCreate, OrgUserSchemaGet } from "reduxConfigs/actions/restapi/orgUserAction";
import { restapiClear } from "reduxConfigs/actions/restapi";
import DragUploadComponent from "../../../Shared/DragUploadComponent/DragUploadComponent";
import { configureAttributeDefaultDataSource } from "../../../Constants";

interface ConfigureUsersProps {
  type: string;
  onClose: () => void;
  exportSampleCsvHandler: () => void;
  exportExistingUsersHandler: () => void;
}

export const ConfigureUsers: React.FC<ConfigureUsersProps> = (props: ConfigureUsersProps) => {
  const { type, onClose, exportExistingUsersHandler, exportSampleCsvHandler } = props;

  const [currentStep, setCurrentStep] = useState<number>(0);
  const [isEditable, setIsEditable] = useState<boolean>(type === "attribute");
  const [loading, setLoading] = useState<boolean>(false);
  const [csvUploaded, setCsvUploaded] = useState<boolean>(false);

  const dispatch = useDispatch();

  const [customFields, setCustomFields] = useState<any[]>([]);

  const userSchemaState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_schema",
    method: "get",
    id: ORG_USER_SCHEMA_ID
  });

  const userSchemaCreateState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_schema",
    method: "create",
    id: "new_schema_set"
  });

  useEffect(() => {
    if (!get(userSchemaState, ["data", "fields"], []).length) {
      dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema"));
      setLoading(true);
    }
    return () => {
      dispatch(restapiClear("org_users_schema", "create", -1));
    };
  }, []);

  useEffect(() => {
    const loading = get(userSchemaState, "loading", true);
    const error = get(userSchemaState, "error", true);
    if (!loading && !error) {
      const data = get(userSchemaState, ["data", "fields"], []);
      const updatedData = data
        .filter((item: any) => !["full_name", "email", "start_date", "integration"].includes(item.key))
        .sort((a: any, b: any) => a.index - b.index)
        .map((item: any) => {
          return {
            ...item,
            title: item?.display_name
          };
        });
      setCustomFields(updatedData);
      setLoading(false);
    }
  }, [userSchemaState]);

  useEffect(() => {
    if (loading) {
      const isLoading = get(userSchemaCreateState, "loading", true);
      const error = get(userSchemaCreateState, "error", true);
      if (!isLoading && !error) {
        const version = get(userSchemaCreateState, ["data", "version"], undefined);
        if (version) {
          setLoading(false);
          if (type === "attribute") {
            dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema"));
            onClose();
          } else {
            setIsEditable(false);
          }
        }
      }
    }
  }, [userSchemaCreateState]);

  const handleCancel = () => {
    onClose();
  };

  const nextHandler = () => {
    setCurrentStep(state => state + 1);
  };

  const backHandler = () => {
    if (currentStep >= 1) {
      dispatch(restapiClear("org_users_import", "create", -1));
    }
    setCurrentStep(state => state - 1);
  };

  const saveHandler = useCallback(() => {
    if (type === "attribute" || (currentStep === 0 && isEditable)) {
      const defaultFieldsData = configureAttributeDefaultDataSource.map((item: any, index) => {
        return {
          index: index + 1,
          key: item.key,
          type: "string",
          display_name: item.title,
          description: item.description
        };
      });
      const customFieldsData = customFields.map((item: any) => {
        const title = item.title;
        return {
          index: item.index,
          key: (title || "").replaceAll(" ", "_").toLowerCase(),
          type: item.type || "text",
          display_name: title,
          description: item?.description
        };
      });
      const data = {
        fields: [...defaultFieldsData, ...customFieldsData]
      };
      dispatch(OrgUserSchemaCreate(data, "new_schema_set"));
      setLoading(true);
    }
  }, [type, customFields, isEditable]);

  const nextButton = useMemo(() => {
    return (
      <AntButton type={"primary"} onClick={nextHandler}>
        Next
      </AntButton>
    );
  }, []);

  const backbutton = useMemo(() => {
    return (
      <AntButton type={"secondary"} onClick={backHandler}>
        Back
      </AntButton>
    );
  }, [currentStep]);

  const closebutton = useMemo(() => {
    return (
      <AntButton type={"primary"} onClick={handleCancel}>
        Close
      </AntButton>
    );
  }, [currentStep]);

  const cancelbutton = useMemo(() => {
    return (
      <AntButton type={"secondary"} onClick={handleCancel}>
        Close
      </AntButton>
    );
  }, []);

  const savebutton = useMemo(() => {
    let disabled = false;
    if (type === "attribute" || (!currentStep && isEditable)) {
      const invalidFields = customFields.filter((item: any) => {
        const title = item.title.replace(" ", "");
        return !title.length;
      });

      disabled = !!invalidFields.length;
    }
    return (
      <AntButton type={"primary"} disabled={disabled} onClick={saveHandler}>
        Save
      </AntButton>
    );
  }, [currentStep, type, customFields, isEditable]);

  const renderTitle = useMemo(() => {
    if (type === "attribute") {
      return "Configure Contributor Attributes";
    }
    return "Import Contributors from a CSV File";
  }, [type]);

  const renderFooter = useMemo(() => {
    if (currentStep === 0 && isEditable) {
      return [cancelbutton, savebutton];
    } else if (currentStep === 0 && !isEditable) {
      return [cancelbutton, nextButton];
    } else if (currentStep !== 0 && !isEditable && csvUploaded) {
      return [closebutton];
    } else {
      return [backbutton, closebutton];
    }
  }, [type, currentStep, customFields, isEditable, csvUploaded]);

  const renderContent = useMemo(() => {
    if (type === "attribute" || currentStep === 0) {
      return (
        <ConfigureAttributes
          isEditable={isEditable}
          setIsEditable={setIsEditable}
          customFields={customFields}
          setCustomFields={setCustomFields}
          exportExistingUsersHandler={exportExistingUsersHandler}
          exportSampleCsvHandler={exportSampleCsvHandler}
        />
      );
    } else {
      return <DragUploadComponent callback={setCsvUploaded} />;
    }
  }, [type, isEditable, customFields, currentStep]);

  return (
    <AntModal title={renderTitle} visible onCancel={handleCancel} width="60vw" footer={renderFooter}>
      {renderContent}
    </AntModal>
  );
};

export default ConfigureUsers;