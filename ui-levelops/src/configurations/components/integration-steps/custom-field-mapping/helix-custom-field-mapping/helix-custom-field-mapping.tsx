import React, { useCallback, useMemo } from "react";
import { Icon, Spin } from "antd";
import { v1 as uuid } from "uuid";
import { getColumns } from "configurations/containers/integration-steps/integrations-details-new/table-config";
import { AntInput, AntTable, AntText, TableRowActions, TitleWithInfo } from "shared-resources/components";
import { HELIX_TITLE_DESCRIPTION, HELIX_TITLE, HELIX_FIELD_MAPPING_NOT_FOUND, HELIX_COLUMN_TYPE } from "./helpers";

interface HelixCustomFieldMappingProps {
  mapping: any;
  loading: boolean;
  onMappingChange: any;
}

const HelixCustomFieldMappingComponent: React.FC<HelixCustomFieldMappingProps> = (
  props: HelixCustomFieldMappingProps
) => {
  const { mapping, loading, onMappingChange } = props;

  const isValid = useMemo(() => {
    return mapping && mapping.every((i: any) => i.repo_id && i.path_prefix);
  }, [mapping]);

  const handleAdd = useCallback(() => {
    const newRepoPathMapping = {
      id: uuid(),
      repo_id: "",
      path_prefix: ""
    };
    isValid && onMappingChange([...mapping, newRepoPathMapping]);
  }, [mapping]);

  const handleFieldChange = useCallback(
    (id: string, type: string) => {
      return (e: any) => {
        const repoPathMapping = mapping.find((item: any) => item.id === id);
        if (repoPathMapping) {
          const index = mapping.indexOf(repoPathMapping);
          repoPathMapping[type] = e.target.value;
          mapping[index] = repoPathMapping;
          onMappingChange([...mapping]);
        }
      };
    },
    [mapping]
  );

  const handleDelete = useCallback(
    (id: string) => {
      onMappingChange(mapping.filter((field: { id: any }) => field.id !== id));
    },
    [mapping]
  );

  const buildAction = useCallback(
    (record: any) => {
      const action = [
        {
          type: "delete",
          id: record.id,
          onClickEvent: handleDelete
        }
      ];
      return <TableRowActions actions={action} />;
    },
    [mapping]
  );

  const buildRepoPathField = useCallback(
    (record: any) => {
      return (
        <AntInput
          className="w-100"
          value={record.repo_id}
          onChange={handleFieldChange(record.id, "repo_id")}
          placeholder="Repo Name"
          hasError={!record.repo_id}
        />
      );
    },
    [mapping]
  );

  const buildDepotPathField = useCallback(
    (record: any) => {
      return (
        <AntInput
          className="w-100"
          value={record.path_prefix}
          onChange={handleFieldChange(record.id, "path_prefix")}
          placeholder="Depot Path"
          hasError={!record.path_prefix}
        />
      );
    },
    [mapping]
  );

  const columns = useMemo(() => {
    let columns: any = getColumns(HELIX_COLUMN_TYPE);
    return columns.map((col: any) => {
      if (col.key === "id") {
        return {
          ...col,
          render: (item: any, record: any) => buildAction(record)
        };
      }
      if (col.key === "repo_id") {
        return {
          ...col,
          render: (item: any, record: any) => buildRepoPathField(record)
        };
      }
      if (col.key === "path_prefix") {
        return {
          ...col,
          render: (item: any, record: any) => buildDepotPathField(record)
        };
      }
      return col;
    });
  }, [mapping]);

  const renderHeader = useMemo(() => {
    return <TitleWithInfo title={HELIX_TITLE} description={HELIX_TITLE_DESCRIPTION} />;
  }, []);

  const renderTable = useMemo(() => {
    if (!mapping.length) {
      return <AntText className="no-mapping-text">{HELIX_FIELD_MAPPING_NOT_FOUND}</AntText>;
    }

    return (
      <div className="custom-mapping-table-container">
        <AntTable bordered={false} columns={columns} dataSource={mapping} pagination={false} />
      </div>
    );
  }, [columns, mapping]);

  const renderActionButton = useMemo(() => {
    const btnClass = isValid ? "add-repo-btn" : "add-repo-btn add-repo-btn-disabled";

    return (
      <div onClick={handleAdd} className={btnClass}>
        <Icon className="add-icon" type="plus-circle" />
        Add Repo
      </div>
    );
  }, [mapping]);

  if (loading) {
    return (
      <div className="spinner">
        <Spin />
      </div>
    );
  }

  return (
    <div className="custom-fields-mapping">
      {renderHeader}
      {renderTable}
      {renderActionButton}
    </div>
  );
};

export default HelixCustomFieldMappingComponent;
