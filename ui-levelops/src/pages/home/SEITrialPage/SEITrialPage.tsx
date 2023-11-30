/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import React from "react";
import { Heading, Layout, Container, Text, Button } from "@harness/uicore";
import { Color } from "@harness/design-system";
import bgImage from "../images/sei.png";
import "./SEITrialPage.scss";

const SALES_URL =
  "https://www.harness.io/company/contact-sales?utm_source=Website&utm_medium=internal&utm_content=Contact-Sales-STO-Pricing";

const SEITrialPage: React.FC = () => (
  <Container className={"body"} style={{ background: `transparent url(${bgImage}) no-repeat` }}>
    <Heading className={"heading"} font={{ weight: "bold", size: "large" }} color={Color.BLACK_100}>
      {"Software Engineering Insights"}
    </Heading>
    <Layout.Vertical spacing="small">
      <Text padding={{ bottom: "xxlarge" }} width={500}>
        {
          "Improve software quality, developer experience, and time to value by gaining actionable insights into software delivery, workflows, and systems."
        }
      </Text>
      <a href="https://www.harness.io/products/software-engineering-insights" rel="noreferrer" target="_blank">
        {" Learn more"}
      </a>
      <Button width={300} height={45} intent="primary" target="_blank" href={SALES_URL} text={"Contact Sales"} />
    </Layout.Vertical>
  </Container>
);
export default SEITrialPage;
