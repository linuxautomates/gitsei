import { WidgetFilterContext, widgetOtherKeyDataContext } from "dashboard/pages/context";
import { get } from "lodash";
import React, { useContext } from "react";
import { AntIcon, AntText, AntTooltip } from "shared-resources/components";
import "./jiraReleaseReportTitle.scss";
import moment from "moment";
import { DateFormats, getTimeForTrellisProfile } from "utils/dateUtils";

interface JiraReportTitleProps {
    title: string;
    widgetId: string;
    widgetRef?: any;
    titleRef: any;
    titleStyle: any;
    description?: string;
}

const JiraReportTitle = (props: JiraReportTitleProps) => {
    const { widgetRef, title, widgetId, titleRef, titleStyle, description } = props;
    const { otherKeyData } = useContext(widgetOtherKeyDataContext);
    const lastUpdatedTime = get(otherKeyData, [widgetId, "lastUpdatedTime"], "");
    const lastUpdatedTimeConvert = lastUpdatedTime ? getTimeForTrellisProfile(lastUpdatedTime) :  moment().format(DateFormats.DAY_TIME);

    return (
        <div className="flex justify-start mr-6 jiraReportTitle direction-column">
            <div className="flex nameContainer">
                <AntTooltip title={title} trigger="hover" getTooltipContainer={() => widgetRef.current}>
                    <div className="titleJiraClass">
                        {title}
                    </div>
                </AntTooltip>
                {description && (
                    <span className="description-jira-icon">
                        <AntTooltip
                            title={description}
                            trigger={["hover", "click"]}
                            getPopupContainer={(trigger: any) => widgetRef.current}>
                            <AntIcon type="info-circle" theme="outlined" />
                        </AntTooltip>
                    </span>
                )}
            </div>
            <div className="dateFild">
                Last Updated at {lastUpdatedTimeConvert}
            </div>
        </div>
    );
};

export default JiraReportTitle;

export const getJiraReleaseTitle = (args: any) => {
    return (
        <JiraReportTitle
            title={args.title}
            widgetId={args.widgetId}
            widgetRef={args.widgetRef}
            titleRef={args.titleRef}
            titleStyle={args.titleStyle}
            description={args.description}
        />
    );
};
