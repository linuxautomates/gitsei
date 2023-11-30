import { Container, FontVariation, Text } from "@harness/uicore";
import React from "react";
import NoAccessImage from "./images/RBACNoAccess.png";
import "./RBACNoAccessScreen.scss";

const RBACNoAccessScreen = () => (
  <Container width={500} className="rbacNoAccessScreen">
    <img src={NoAccessImage} />
    <Container className="textDesc">
      <Text font={{ variation: FontVariation.H4 }}>You don’t have the permissions to access this page</Text>
      <Text font={{ variation: FontVariation.BODY }} width={415} className="alignCenter">
        To access this page please contact your admin to give required permissions
      </Text>
    </Container>
  </Container>
);

export default RBACNoAccessScreen;
