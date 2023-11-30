import { Container, Text, Card } from "@harness/uicore";
import React from "react";
import "./GetStarted.scss";
import { Classes } from "@blueprintjs/core";
import GetStartedModal from "./GetStartedModal";

const GetStarted = (): JSX.Element => {
  return (
    <Container padding="huge" className="getStartedContainer">
      <Container className="widgetContainer">
        <Card className="widget">
          <Text margin={{ bottom: "huge", top: "small" }} className={Classes.SKELETON}>
            Lead Time For Changes
          </Text>
          <Container height={22} width={300} className={Classes.SKELETON} />
          <Container height={22} width={250} className={Classes.SKELETON} />
          <Container height={22} width={350} className={Classes.SKELETON} />
          <Container height={22} width={320} className={Classes.SKELETON} />
          <Container height={22} width={360} className={Classes.SKELETON} />
        </Card>
        <Card className="widget">
          <Text margin={{ bottom: "huge", top: "small" }} className={Classes.SKELETON}>
            Lead Time For Changes
          </Text>
          <Container height={22} width={500} className={Classes.SKELETON} />
          <Container height={22} width={500} className={Classes.SKELETON} />
        </Card>
      </Container>
      <Card>
        <Container height={22} width={500} className={Classes.SKELETON} margin={{ bottom: "medium" }} />
        <Container height={22} width={500} className={Classes.SKELETON} />
      </Card>
      <GetStartedModal />
    </Container>
  );
};

export default GetStarted;
