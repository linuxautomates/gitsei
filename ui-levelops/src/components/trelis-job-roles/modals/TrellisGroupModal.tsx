import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Divider, Icon, Modal, Switch } from "antd";
import { AntInput, AntSelect, SvgIcon } from "shared-resources/components";
import "./TrellisGroupModal.scss";
import OrgUnitAttributeSelection from "configurations/pages/Organization/organization-unit/OrgUnitAttributeSelection";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOrgUnitUtility } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { cloneDeep } from "lodash";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { toTitleCase } from "utils/stringUtils";

interface Criteria {
  condition: string;
  field: string;
  options?: Array<Record<string, any>>;
  values: Array<string>;
}[]
interface Group {
  name: string;
  matchingCriteria: Array<Criteria>;
}
export interface TrellisGroupModalProps {
  visible: boolean;
  onClose: () => void;
  handleTrellisGroup: (param: Group) => void;
  profile: Group;
  profileNames: Array<string>
}

export const TrellisGroupModal: React.FC<TrellisGroupModalProps> = ({
  visible = false,
  onClose,
  handleTrellisGroup,
  profile,
  profileNames
}) => {
  const [groupForm, setGroupForm] = useState<Group>({
    name: profile?.name ? profile?.name : "",
    matchingCriteria:
      profile?.name && profile.matchingCriteria?.length > 0
        ? profile.matchingCriteria
        : [{ condition: "", field: "", options: [], values: [] }]
  });
  const [showErrorMsg, setShowErrorMsg] = useState<boolean>(false);
  const [isProfileNameExists, setIsProfileNameExists] = useState<boolean>(false);

  const utilityLoading = useParamSelector(getOrgUnitUtility, { utility: "loading" });
  const userAttributesOptions = useParamSelector(getOrgUnitUtility, { utility: "custom_attributes" });

  useEffect(() => {
    if (profile?.name) {
      const newGroup: Group = cloneDeep(groupForm);
      const criteria: any = profile.matchingCriteria?.map(crt => {
        const findOptions = userAttributesOptions.find((obj: any) => obj.value === crt.field);
        if (findOptions) {
          return {
            ...crt,
            options: findOptions?.options || []
          };
        }
        return {
          ...crt
        };
      });
      newGroup.matchingCriteria =
        criteria?.length === 0 ? [{ condition: "", field: "", options: [], values: [] }] : criteria;
      setGroupForm(newGroup);
    }
  }, []);

  const customFields = useMemo(() => {
    let fields = userAttributesOptions.filter(
      (options: Record<string, any>) => !["email", "full_name", "custom_field_integration"].includes(options.value)
    );
    fields = fields?.map((obj: Record<string, any>) => {
      return { ...obj, label: toTitleCase(obj.label) };
    });
    return fields;
  }, [userAttributesOptions]);

  const handleAttributeChange = useCallback(
    (val, index) => {
      const checkAlreadyPresent = groupForm.matchingCriteria?.find(opt => opt.field === val);
      const newGroup: Group = cloneDeep(groupForm);
      const newOpt = customFields?.find((newOptions: Record<string, any>) => newOptions.value === val);
      if (val !== newGroup?.matchingCriteria?.[index]?.field) {
        newGroup.matchingCriteria[index].values = [];
      }
      newGroup.matchingCriteria[index].field = val;
      newGroup.matchingCriteria[index].options = newOpt.options;
      setGroupForm(newGroup);
      if (checkAlreadyPresent) {
        setShowErrorMsg(true);
      } else {
        setShowErrorMsg(false);
      }
    },
    [customFields, groupForm, setGroupForm]
  );

  const handleOptionChange = useCallback(
    (val, index: number, key: string) => {
      const newGroup: any = cloneDeep(groupForm);
      newGroup.matchingCriteria[index][key] = val;
      setGroupForm(newGroup);
    },
    [setGroupForm, groupForm]
  );

  const handleDelete = useCallback(
    rowId => {
      const newGroup: Group = cloneDeep(groupForm);
      const counter: any = {};
      newGroup.matchingCriteria = newGroup.matchingCriteria.filter(
        (criteria: Record<string, any>, index: number) => index !== rowId
      );
      const check = newGroup.matchingCriteria?.map(opt => opt.field);
      const duplicates = check.filter(n => (counter[n] = counter[n] + 1 || 1) === 2);
      if (duplicates.length > 0) {
        setShowErrorMsg(true);
      } else {
        setShowErrorMsg(false);
      }
      setGroupForm(newGroup);
    },
    [groupForm]
  );

  const handleNameChange = useCallback(
    name => {
      const profileNameExists = profileNames.find(profileName => profileName?.toLowerCase() === name?.toLowerCase());
      const newGroup: Group = cloneDeep(groupForm);
      newGroup.name = name;
      setGroupForm(newGroup);
      setIsProfileNameExists(!!profileNameExists);
    },
    [groupForm, profileNames, isProfileNameExists]
  );

  const handleAddCondition = useCallback(() => {
    const newGroup: Group = cloneDeep(groupForm);
    newGroup.matchingCriteria.push({
      condition: "",
      field: "",
      values: [],
      options: []
    });
    setGroupForm(newGroup);
  }, [groupForm]);

  const isValidForm = useMemo(() => {
    const { field = "", values = [], condition = "" } = groupForm?.matchingCriteria?.[0] || {};
    return (
      isProfileNameExists ||
      showErrorMsg ||
      !groupForm?.name ||
      field?.length === 0 ||
      values?.length === 0 ||
      condition?.length === 0
    );
  }, [groupForm, isProfileNameExists]);

  const handleSave = useCallback(() => {
    const newGroup: Group = cloneDeep(groupForm);
    newGroup.matchingCriteria = newGroup.matchingCriteria
      .filter(obj => {
        return !(obj.field?.length === 0 || obj.values?.length === 0 || obj.condition?.length === 0);
      })
      ?.map(data => {
        delete data.options;
        return {
          ...data,
          field: data?.field?.replace("custom_field_", "")
        };
      });
    handleTrellisGroup(newGroup);
  }, [groupForm]);

  return (
    <Modal
      title={`Configure ${profile?.name ? "Edit" : "New"} Trellis Group`}
      visible={visible}
      className="trellis-group-modal"
      cancelText="Cancel"
      okText="Save"
      width={"57rem"}
      maskClosable={false}
      footer={[
        <Button disabled={isValidForm} type="primary" onClick={handleSave}>
          Save
        </Button>,
        <Button key="submit" onClick={onClose}>
          Cancel
        </Button>
      ]}
      onCancel={onClose}>
      <div className="row">
        <label className="title-label">Trellis Group name</label>
        <AntInput onChange={(e: any) => handleNameChange(e?.target?.value)} value={groupForm?.name} />
        {isProfileNameExists && <div className="error-msg">Trellis Group name already exists.</div>}
      </div>
      <div className="title">Define Trellis Group</div>

      <div className="row-header">
        <span className="title-label">User Attribute</span>
        <span className="title-label">Conditions</span>
        <span className="title-label">Value</span>
      </div>
      <Divider />
      <div>
        {groupForm?.matchingCriteria?.map((criteria, index) => {
          return (
            <>
              <div className="conditions" key={`${criteria?.field?.[0]}_${index}`}>
                <AntSelect
                  options={customFields || []}
                  value={criteria?.field}
                  onChange={(values: string) => handleAttributeChange(values, index)}
                />
                <AntSelect
                  options={[
                    { label: "!=", value: "NEQ" },
                    { label: "==", value: "EQ" }
                  ]}
                  onChange={(selectedOption: string) => handleOptionChange(selectedOption, index, "condition")}
                  value={criteria?.condition}
                />
                <AntSelect
                  onChange={(selectedOption: string[]) => handleOptionChange(selectedOption, index, "values")}
                  options={criteria.options || []}
                  mode="multiple"
                  value={criteria?.values}
                  maxTagCount={1}
                />
                <Button
                  disabled={groupForm?.matchingCriteria?.length === 1}
                  onClick={() => handleDelete(index)}
                  icon="delete"
                />
              </div>
              {groupForm?.matchingCriteria?.length > 1 && groupForm?.matchingCriteria?.length - 1 !== index && (
                <Divider>AND</Divider>
              )}
            </>
          );
        })}
        {showErrorMsg && <div className="error-msg">User attribute already present</div>}
      </div>
      <Button icon="plus-circle" type="link" disabled={isValidForm} className="" onClick={handleAddCondition}>
        New Condition
      </Button>
    </Modal>
  );
};
