import {
  Button,
  ButtonVariation,
  Color,
  Container,
  FontVariation,
  Formik,
  FormInput,
  Layout,
  StepProps,
  Text
} from "@harness/uicore";
import CardV2 from "components/CardV2/CardV2";
import React from "react";
import css from "./Configuration.module.scss";
import { NameIdDescriptionTags } from "components/NameIdDescriptionTags/NameIdDescriptionTags";
import { JiraSelfManagedDTO } from "./Configuration.types";
import { JIRA_SELFMANAGED_CONFIGURATION_FIELDS, JIRA_SELFMANAGED_INITIAL_VALUES } from "./Configuration.constants";
import { validateSelfManagedJiraConfigurations } from "./Configurations.utils";

interface ConfigurationProps {
  integrationType: string;
  name: string;
  subtitle: string;
}

export default function Configuration(props: StepProps<JiraSelfManagedDTO> & ConfigurationProps): JSX.Element {
  const { nextStep, prevStepData } = props;

  return (
    <Layout.Vertical className={css.configurationPanel}>
      <Container>
        <Text font={{ variation: FontVariation.H4 }}>{"Overview"}</Text>
        <Text
          font={{ variation: FontVariation.BODY2, size: "normal", weight: "light" }}
          color={Color.GREY_500}
          padding={{ top: "small", bottom: "small" }}>
          {"Provide basic overview information of your integration of easier management"}
        </Text>
        <Formik<JiraSelfManagedDTO>
          initialValues={prevStepData ?? JIRA_SELFMANAGED_INITIAL_VALUES}
          enableReinitialize={false}
          formName="jiraSelfManaged"
          onSubmit={data => {
            nextStep?.(data);
          }}
          validate={formData => validateSelfManagedJiraConfigurations(formData)}>
          {formikProps => (
            <>
              <CardV2>
                <NameIdDescriptionTags
                  formikProps={formikProps}
                  className={css.jiraSelfManagedFields}
                  identifierProps={{
                    inputLabel: "Integration Name"
                  }}
                  tooltipProps={{ dataTooltipId: "NameIdDescriptionTagsHealthSource" }}
                  isIdentifierHidden
                />
              </CardV2>
              <Text font={{ variation: FontVariation.H4 }} padding={{ top: "large", bottom: "medium" }}>
                {"Provide the JIRA details to fetch data"}
              </Text>
              <CardV2>
                <Text font={{ variation: FontVariation.BODY2 }} padding={{ top: "small", bottom: "medium" }}>
                  {"Authentication"}
                </Text>
                <FormInput.Text
                  name={JIRA_SELFMANAGED_CONFIGURATION_FIELDS.JIRA_URL}
                  placeholder={"Enter Jira URL"}
                  label={"Jira URL"}
                  className={css.jiraSelfManagedFields}
                />
                <FormInput.Text
                  name={JIRA_SELFMANAGED_CONFIGURATION_FIELDS.USERNAME}
                  placeholder={"Enter Username"}
                  label={"Username"}
                  className={css.jiraSelfManagedFields}
                />
                <FormInput.Text
                  name={JIRA_SELFMANAGED_CONFIGURATION_FIELDS.APIKEY}
                  inputGroup={{ type: "password" }}
                  placeholder={"Enter API Key"}
                  label={"API Key"}
                  className={css.jiraSelfManagedFields}
                />
                <Layout.Vertical>
                  <Text font={{ variation: FontVariation.BODY2 }} padding={{ top: "medium", bottom: "small" }}>
                    {"Provide the JQL query to filter issues for ingestion (Optional)"}
                  </Text>
                  <Text font={{ variation: FontVariation.SMALL }} padding={{ bottom: "medium" }} color={Color.GREY_500}>
                    {"Only issues matched by that query will be ingested by SEI. Leave blank to ingest everything."}
                  </Text>
                  <FormInput.TextArea
                    className={css.jqlQuery}
                    textArea={{
                      style: { maxHeight: 100 }
                    }}
                    name={JIRA_SELFMANAGED_CONFIGURATION_FIELDS.JQL_QUERY}
                    placeholder={'e.g project = "Project Name" AND status = "In Progress" AND assignee = username'}
                  />
                </Layout.Vertical>
              </CardV2>
              <Container>
                <Button
                  variation={ButtonVariation.PRIMARY}
                  type="submit"
                  text={"Validate Connection"}
                  rightIcon="chevron-right"
                  className={css.nextBtn}
                  onClick={() => formikProps.submitForm()}
                />
              </Container>
            </>
          )}
        </Formik>
      </Container>
    </Layout.Vertical>
  );
}
