import { Button, ButtonVariation, Container, Icon, ModalDialog, Text } from "@harness/uicore";
import { getIntegrationMappingPage } from "constants/routePaths";
import getStartedImage from "./images/getStartedImage.png";
import { FontVariation } from "@harness/design-system";
import React from "react";
import { useHistory } from "react-router-dom";
import "./GetStartedModal.scss";

const GetStartedModal = () => {
  const history = useHistory();
  return (
    <ModalDialog
      isOpen={true}
      enforceFocus={true}
      width={900}
      canEscapeKeyClose={false}
      canOutsideClickClose={false}
      isCloseButtonShown={false}>
      <Container className="dialogContainer">
        <Container className={"getStarted"} width={390}>
          <Container className={"header"}>
            <Icon name={"sei-main"} size={48} />
            <Text font={{ variation: FontVariation.H3 }} margin={{ top: "medium", buttom: "small" }}>
              {"Gain Insights and Visibility accross your SDLC process"}
            </Text>
            <Text font={{ variation: FontVariation.BODY }}>
              {
                "Discover bottlenecks, assess developer and team productivity, and improve developer experience guided by data and insights"
              }
            </Text>
          </Container>
          <Container>
            <Button
              height={32}
              rightIcon={"chevron-right"}
              text={"Get Started by mapping integrations"}
              variation={ButtonVariation.PRIMARY}
              onClick={() => history.push(getIntegrationMappingPage())}
            />
          </Container>
        </Container>
        <img width={322} height={285} src={getStartedImage} className="image" />
      </Container>
    </ModalDialog>
  );
};

export default GetStartedModal;
