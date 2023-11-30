import React, { useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { get } from "lodash";
import { RouteChildrenProps } from "react-router-dom";
import { ServerPaginatedTable } from "shared-resources/containers";
import Loader from "../../../components/Loader/Loader";
import { tableColumns } from "./automated-rules-list.table-config";
import { automationRuleDeleteState } from "reduxConfigs/selectors/restapiSelector";
import { automationRulesDelete, restapiClear } from "reduxConfigs/actions/restapi";
import { ConfirmationModal, TableRowActions } from "shared-resources/components";
import { useAutomationRules } from "./useAutomationRules";
import { notification } from "antd";

export interface AutomationRulesListContainerProps extends RouteChildrenProps {}

const AutomationRulesListContainer: React.FC<AutomationRulesListContainerProps> = (
  props: AutomationRulesListContainerProps
) => {
  const dispatch = useDispatch();
  const [reload, setReload] = useState<number>(1);

  // Load Automation Rules stuff.
  const { loading: loadingAutomationRulesStuff } = useAutomationRules();

  const moreFilters = useRef<any>({});
  const partialFilters = useRef<any>({});

  // Delete
  const [deleteAutomationRuleId, setDeleteAutomationRuleId] = useState<string | undefined>();
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const deleteState = useSelector(state => automationRuleDeleteState(state, deleteAutomationRuleId));
  React.useEffect(
    () => {
      if (deleteLoading) {
        const loading = get(deleteState, ["loading"], true);
        const error = get(deleteState, ["error"], true);
        if (!loading && !error) {
          const data = get(deleteState, ["data"], {});
          if (!data["success"]) {
            notification.error({
              message: data["error"]
            });
            setDeleteLoading(false);
            setDeleteAutomationRuleId(undefined);
          } else {
            setDeleteLoading(false);
            setDeleteAutomationRuleId(undefined);
            setReload((prev: number) => prev + 1);
          }
        }
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [deleteState]
  );
  const onRemoveHandler = (automationRuleId: string) => {
    setDeleteAutomationRuleId(automationRuleId);
    setDeleteLoading(true);
    dispatch(automationRulesDelete(automationRuleId));
  };

  // Clear stuff
  React.useEffect(
    () => {
      return () => {
        dispatch(restapiClear("automation_rules", "get", 0));
      };
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );

  const buildActionOptions = (record: any) => {
    const actions = [
      {
        type: "delete",
        id: record.id,
        description: "Delete",
        onClickEvent: onRemoveHandler
      }
    ];

    return <TableRowActions actions={actions} />;
  };

  const mappedColumns = tableColumns().map(column => {
    if (column.dataIndex === "id") {
      return {
        ...column,
        width: 100,
        render: (text: string, record: any, index: number) => {
          return buildActionOptions(record);
        }
      };
    }
    return column;
  });

  if (loadingAutomationRulesStuff || deleteLoading) {
    return <Loader />;
  }

  return (
    <>
      <ServerPaginatedTable
        pageName={"automation_rules"}
        uri={"automation_rules"}
        partialFilters={partialFilters.current}
        columns={mappedColumns}
        hasFilters={true}
        moreFilters={moreFilters.current}
        reload={reload}
      />
    </>
  );
};

export default AutomationRulesListContainer;
