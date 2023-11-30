import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Timeline, Tooltip } from "antd";
import "./auditlogs.style.scss";
import moment from "moment";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { getActivityLogsSelector } from "reduxConfigs/selectors/restapiSelector";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { activitylogsList } from "reduxConfigs/actions/restapi/activitylogActions";

interface AuditLogsProps {
  workItemId: string;
}

const AuditLogsContainer: React.FC<AuditLogsProps> = props => {
  const dispatch = useDispatch();
  const [work_item_id, setWorkItemId] = useState<undefined | string>(undefined);

  const audits = useSelector(state => getActivityLogsSelector(state));

  useEffect(() => {
    if (props.workItemId !== work_item_id) {
      dispatch(activitylogsList({ filter: { target_items: [props.workItemId] } }));
      setWorkItemId(props.workItemId);
    }
  }, [props]);

  const getIcon = (type: string) => {
    switch (type) {
      case "KB":
        return "knowledgeBase";
      case "ASSESSMENT":
        return "assessments";
      case "WORKFLOW":
      default:
        return "workflows";
    }
  };

  const getIconComponent = (type: string) => (
    <div className="audit-log-icon-container">
      <SvgIconComponent className={["audit-log-icon", `type-${type}`].join(" ")} icon={getIcon(type)} />
    </div>
  );

  return (
    <div className="audit-logs">
      <Timeline>
        {audits &&
          audits.length &&
          audits.map((note: any, index: number) => (
            <Timeline.Item key={`log-${index}`} dot={getIconComponent(note.target_item_type)}>
              <div className="auditlog-body">
                {note.body}
                <p className="author">
                  {note.email}
                  <span className="pl-5 pr-5">&bull;</span>
                  <Tooltip
                    className="date"
                    title={
                      // @ts-ignore
                      moment().format("YYYY-MM-DD HH:mm:ss")
                    }>
                    <span>{moment.unix(note.created_at).fromNow()}</span>
                  </Tooltip>
                </p>
              </div>
            </Timeline.Item>
          ))}
      </Timeline>
    </div>
  );
};

export default AuditLogsContainer;
