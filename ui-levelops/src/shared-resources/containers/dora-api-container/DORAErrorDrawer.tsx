import { Drawer } from 'antd';
import React, { useMemo } from 'react';
import { AntButton, AntText, SvgIcon } from 'shared-resources/components';
import "./DORAErrorDrawer.scss";

interface DORAErrorDrawerProps {
  isVisible: boolean,
  onClose: () => void,
  widgetTitle: string,
  errorMessage: string,
  reasons: string[]
}

const DORAErrorDrawer: React.FC<DORAErrorDrawerProps> = ({
  isVisible,
  onClose,
  widgetTitle,
  errorMessage,
  reasons
}) => {
  const title = useMemo(() => (
    <div className="drawer-title">
      <SvgIcon className="reports-btn-icon d-inline-block" icon="questionCircle" />
      <AntText className="text"> Possible Reasons</AntText>
    </div>
  ), []);
  return (
  <Drawer
    title={title}
    width={660}
    visible={isVisible}
    onClose={onClose}
    className="dora-error-drawer"
    bodyStyle={{ paddingBottom: 80 }}
  >
    <AntText className="widget-sub-title">{widgetTitle}</AntText>
    <div className="error-message">
      <SvgIcon className="reports-btn-icon d-inline-block" icon="error" />
      <AntText className="text" >
        {errorMessage}
      </AntText>
    </div>
    <AntText>The following are a few potential causes,</AntText>
    <ol>
      {reasons.map((reason: string) => <li>{reason}</li>)}
    </ol>
    <div  className='footer'>
      <AntButton onClick={onClose}  type="primary">
        Close
      </AntButton>
    </div>
  </Drawer>);
}

export default DORAErrorDrawer;