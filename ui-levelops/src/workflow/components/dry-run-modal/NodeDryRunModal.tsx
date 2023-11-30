import { Button, Modal, notification, Spin, Typography } from "antd";
import { CONIFG_TABLE_FILTER_TYPE, DATE, KV_TYPE, ASSESSMENT_CHECK_TYPE } from "constants/fieldTypes";
import { cloneDeep, get } from "lodash";
import React, { FC, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { evaluateNode, restapiClear } from "reduxConfigs/actions/restapi";
import { getPropelNodesEvaluateSelector } from "reduxConfigs/selectors/propels.selectors";
import { GenericFormComponent } from "shared-resources/containers";
import { toTitleCase } from "utils/stringUtils";
import { nodeInputTransformer } from "../node-configurations/helper";
import "./NodeDryRunModal.scss";

const { Text } = Typography;

interface NodeDryRunModalProps {
  visible: boolean;
  onCancel: () => void;
  node: any;
  predicates?: any;
}

const NodeDryRunModal: FC<NodeDryRunModalProps> = (props: NodeDryRunModalProps) => {
  const dispatch = useDispatch();

  const nodeEvaluteState = useSelector(getPropelNodesEvaluateSelector);

  const [values, setValues] = useState<any>({});
  const [evaluateState, setEvaluteState] = useState<any>({});
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    const {
      node: { input }
    } = props;
    let values: any = {};
    Object.keys(input || {}).forEach(field => {
      values[field] = { value: input[field].values };
    });
    setValues(values);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const { loading, error } = get(nodeEvaluteState, ["0"], { loading: true, error: true });
    if (!loading && !error) {
      setEvaluteState(nodeEvaluteState["0"]?.data || {});
      setLoading(false);
    }

    if (!loading && error) {
      setEvaluteState({});
      setLoading(false);
    }
  }, [nodeEvaluteState]);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("propels_nodes_evaluate", "list", "0"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleChanges = (values: any) => {
    setValues((prev: any) => ({ ...prev, ...values }));
  };

  const getValueType = (fieldType: string) => {
    switch (fieldType) {
      case CONIFG_TABLE_FILTER_TYPE:
        return "json_blob";
      case DATE:
        return "date";
      case KV_TYPE:
      case ASSESSMENT_CHECK_TYPE:
        return "json";
      default:
        return "string";
    }
  };

  const handleEvaluate = () => {
    let input: any = {};
    Object.keys(props.node?.input || {}).forEach(inp => {
      input = {
        ...input,
        [inp]: {
          key: inp,
          value: inp === "parameters" ? values[inp]?.value : values[inp]?.value[0],
          value_type: getValueType(inp)
        }
      };
    });
    const filters = {
      node_type: props.node?.type,
      input
    };
    dispatch(evaluateNode(filters));
    setLoading(true);
  };

  const onEvaluate = () => {
    const regexTocheck = new RegExp("\\$(.)");
    const valuesString = JSON.stringify({ ...values, elements: [] });
    if (!valuesString.match(regexTocheck)) {
      handleEvaluate();
    } else {
      notification.error({
        message: "Use Valid Values"
      });
    }
  };

  const fields = () => {
    const { node, predicates } = props;
    let inputArray = nodeInputTransformer({ node: cloneDeep(node), predicates, values });
    inputArray = inputArray.map(input => {
      return {
        ...input,
        forDryRun: true
      };
    });
    return (
      <GenericFormComponent
        elements={inputArray.sort((a, b) => {
          return a.index - b.index;
        })}
        onChange={handleChanges}
      />
    );
  };

  const buildRow = (heading: string, data: string) => {
    return (
      <div className="mb-10">
        <strong className="mr-10">{heading} :</strong>
        <Text>{data}</Text>
      </div>
    );
  };

  return (
    <Modal
      wrapClassName="node-dry-run-modal"
      title={"Dry Run"}
      footer={null}
      visible={props.visible}
      onCancel={props.onCancel}>
      <div className={"flex justify-content-between"} style={{ padding: "1rem" }}>
        {Object.keys(values).length && (
          <div
            style={{
              width: "60%",
              borderRight: "1px solid var(--grey5)",
              paddingRight: "2rem"
            }}>
            <div
              style={{
                width: "100%",
                height: "30rem",
                overflow: "hidden",
                overflowY: "scroll"
              }}>
              {fields()}
            </div>
            <div className={"py-20"}>
              <Button type="primary" onClick={onEvaluate}>
                Evaluate
              </Button>
            </div>
          </div>
        )}
        <div style={{ width: "38%" }}>
          <strong className="mb-10">Output</strong>
          {loading ? (
            <Spin
              data-testid="output-loading-spinner"
              style={{ display: "flex", justifyContent: "center", alignItems: "center", width: "100%", height: "100%" }}
            />
          ) : (
            <div className={"flex direction-column"} style={{ width: "100%", height: "100%" }}>
              {evaluateState && !evaluateState.success && evaluateState.error && (
                <>
                  <strong className="mb-5" style={{ color: "var(--error)" }}>
                    Error
                  </strong>
                  {buildRow("Type", toTitleCase(evaluateState?.error?.type || ""))}
                  {buildRow("Description", evaluateState?.error?.description)}
                  {buildRow(
                    "Message",
                    evaluateState?.error?.details?.script_error?.message || evaluateState?.error?.details?.message || ""
                  )}
                </>
              )}
              {evaluateState && evaluateState.success && (
                <>
                  <strong className="mb-5" style={{ color: "var(--green6)" }}>
                    Success
                  </strong>
                  <div className="mb-10">
                    {props.node?.type === "script" && (
                      <>
                        <strong className="mr-10">Message :</strong>
                        <Text code>{evaluateState?.node_update?.data?.script_log || ""}</Text>
                      </>
                    )}
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
};

export default NodeDryRunModal;
