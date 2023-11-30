import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { notification, Spin, Modal, Button } from "antd";
import { AntText, AntTable } from "../../../shared-resources/components";
import SlideDown from "react-slidedown";

interface StoreProps {}

interface ExpandedHitsComponentProps extends StoreProps {
  hits: Array<any>;
  stageId: string;
}

// @ts-ignore
export const ExpandedHitsComponent: React.FC<ExpandedHitsComponentProps> = (props: ExpandedHitsComponentProps) => {
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(props.hits.length > 0);
  const [hits, setHits] = useState([]);
  const [hitContent, setHitContent] = useState(undefined);
  const rest_api = useSelector(state => {
    // @ts-ignore
    return get(state.restapiReducer, ["triage_rules", "list", props.stageId], { loading: true, error: false });
  });

  useEffect(() => {
    if (props.hits.length > 0) {
      setLoading(true);
      dispatch(
        genericList(
          "triage_rules",
          "list",
          { filter: { rule_ids: props.hits.map(hit => hit.rule_id) } },
          null,
          props.stageId
        )
      );
    }
    return () => {
      // @ts-ignore
      dispatch(restapiClear("triage_rules", "list", props.stageId));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    console.log(`props hits changed ${loading}`);
    if (!loading) {
      if (props.hits.length > 0) {
        setLoading(true);
        setHits([]);
        dispatch(
          genericList(
            "triage_rules",
            "list",
            { filter: { rule_ids: props.hits.map(hit => hit.rule_id) } },
            null,
            props.stageId
          )
        );
      } else {
        setLoading(false);
        setHits([]);
      }
    }
  }, [props.hits]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (loading) {
      const rulesLoading = get(rest_api, ["loading"], true);
      const rulesError = get(rest_api, ["error"], false);
      if (!rulesLoading) {
        if (rulesError) {
          notification.error({ message: "Could not find rule" });
        } else {
          const rules = get(rest_api, ["data", "records"], []);
          const updatedHits = props.hits.map(hit => {
            const matchingRule = rules.find((rule: { id: any }) => rule.id === hit.rule_id) || {};
            return {
              ...hit,
              rule_name: matchingRule.name
            };
          });
          // @ts-ignore
          setHits(updatedHits);
        }
        setLoading(false);
      }
    }
  }, [rest_api]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    // @ts-ignore
    return (
      // @ts-ignore
      <div align={"center"}>
        <Spin />
      </div>
    );
  }

  const getTable = () => {
    const columns = [
      {
        title: "Rule",
        key: "rule_name",
        dataIndex: "rule_name",
        width: "30%"
      },
      {
        title: "Matches",
        key: "count",
        dataIndex: "count",
        width: "10%"
      },
      {
        title: "Matching Snippet",
        key: "hit_content",
        dataIndex: "hit_content",
        width: "20%",
        render: (item: string, record: any, index: number) => {
          // @ts-ignore
          return (
            // @ts-ignore
            <Button type={"link"} onClick={e => setHitContent(item)}>
              View
            </Button>
          );
        }
      }
    ];
    if (hits.length > 0) {
      return <AntTable dataSource={hits} columns={columns} size={"small"} pagination={false} bordered={false} />;
    }
    return <AntText>No Matches</AntText>;
  };
  return (
    <SlideDown>
      {hitContent && (
        <Modal
          title={"Matching Snippet"}
          width={"80%"}
          bodyStyle={{ maxHeight: "500px", overflowY: "scroll" }}
          footer={null}
          onCancel={(e: any) => setHitContent(undefined)}
          visible>
          <AntText style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}>{hitContent}</AntText>
        </Modal>
      )}
      {getTable()}
    </SlideDown>
  );
};
