import * as React from "react";
import { FlowChart, IChart, IConfig, ILink, IFlowChartComponents, actions } from "@mrblenny/react-flow-chart";
// @ts-ignore
import { mapValues } from "lodash";

import { ILinkDefaultProps, INodeInnerDefaultProps } from "@mrblenny/react-flow-chart/src";

export interface ICustomLinkDefaultProps extends ILinkDefaultProps {
  onEditClick?: (link: ILink) => void;
  onDeleteClick?: (link: ILink) => void;
}

export interface ICustomNodeInnerDefaultProps extends INodeInnerDefaultProps {
  onDelete?: (nodeId: any) => void;
  onSetting?: (nodeId: any) => void;
}

export interface IFlowChartWithStateComponents extends IFlowChartComponents {
  Link?: React.FunctionComponent<ICustomLinkDefaultProps>;
}

export interface IFlowChartWithStateProps {
  initialValue: IChart;
  //Components?: IFlowChartComponents;
  Components?: IFlowChartWithStateComponents;
  config?: IConfig;
  callbacks?: any;
  reload: number;
}

/**
 * Flow Chart With State
 */
export class FlowChartWithState extends React.Component<IFlowChartWithStateProps, IChart> {
  public state: IChart;
  private stateActions = mapValues(actions, (func: any) => (...args: any) => this.setState(func(...args)));

  constructor(props: IFlowChartWithStateProps) {
    super(props);
    this.state = props.initialValue;
  }

  // @ts-ignore
  componentDidUpdate(prevProps: Readonly<IFlowChartWithStateProps>, prevState: Readonly<any>, snapshot?: any): void {
    if (prevProps.reload !== this.props.reload) {
      this.setState(this.props.initialValue);
    }
  }

  public render() {
    const { Components, config, callbacks } = this.props;
    // @ts-ignore
    return (
      <FlowChart
        chart={this.state}
        callbacks={{
          ...this.stateActions,
          // @ts-ignore
          ...callbacks
        }}
        Components={Components}
        config={config}
      />
    );
  }
}
