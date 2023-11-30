import React from "react";
import { Button, ButtonVariation, Container, FontVariation, Text } from "@harness/uicore";
import SeiProcessImage from "@assets/img/SeiProcessImage.png";
import "./ChangeScreenWarning.scss";
import { useHistory } from "react-router-dom";
import { useParentProvider } from "contexts/ParentProvider";
import { useAppStore } from "contexts/AppStoreContext";

const ChangeScreenWarning = ({ setOpen }: { setOpen: (value: boolean) => void }) => {
  const { routes } = useParentProvider();
  const history = useHistory();
  const { accountInfo } = useAppStore();
  const onProceedClick = () => {
    history.push((routes as any).toSEIIntegrations({ accountId: accountInfo?.identifier, module: "sei" }));
  };
  return (
    <Container className="changeScreenWarning">
      <Container className="firstContainer">
        <Text font={{ variation: FontVariation.H4 }}>You are about to be redirected to Account setup</Text>
        <Text>
          SEI Connectors are available only on account scope. Once integration is added to account, you need to do the
          mapping of connectors in the poject scope
        </Text>
        <Container>
          <Button text="Proceed" variation={ButtonVariation.PRIMARY} onClick={onProceedClick} />
          <Button text="Will do it later" variation={ButtonVariation.LINK} onClick={() => setOpen(false)} />
        </Container>
      </Container>
      <img width="118" height="210" src={SeiProcessImage} />
    </Container>
  );
};

export default ChangeScreenWarning;
