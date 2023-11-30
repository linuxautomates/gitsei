import {
  Button,
  ButtonVariation,
  Color,
  Container,
  FontVariation,
  Formik,
  Layout,
  StepProps,
  Text,
  TextInput
} from "@harness/uicore";
import CardV2 from "components/CardV2/CardV2";
import React, { useEffect, useState } from "react";
import css from "./Configuration.module.scss";
import { useCopyToClipboard } from "custom-hooks/useCopyToClipboard";
import { NameIdDescriptionTags } from "components/NameIdDescriptionTags/NameIdDescriptionTags";
import { IntegrationData, JiraCloudConnectionDTO } from "./Configuration.types";
import {
  JIRA_CLOUD_CONNETION_INITIAL_VALUES,
  OTP_EXPIRATION_TIME,
  OTP_NOT_SUBMITTED_STATUS_CODE,
  SEI_APP_URL
} from "./Configuration.constants";
import { getJiraIntegrationPayload, vaidateCloudJiraConfigurations } from "./Configurations.utils";
import { JiraCloudGenerateTokenService, JiraCloudVerifyConnectionService } from "services/restapi";
import { useToaster } from "@harness/uicore";
import { getErrorMessage } from "utils/apiUtils";
import { GetDataError } from "types/Api.types";
import KeyExpirationTimer from "./components/KeyExpirationTimer";
import { JiraCloudCreateIntegration } from "services/restapi/jiraV2/JiraV2.service";
import IntegrationSuccess from "./components/IntegrationSuccess/IntegrationSuccess";
import VerifyingConnectionLoading from "./components/VerifyingConnectionLoading/VerifyingConnectionLoading";

interface ConfigurationProps {
  integrationType: string;
  name: string;
  subtitle: string;
}

export default function Configuration(props: StepProps<JiraCloudConnectionDTO> & ConfigurationProps): JSX.Element {
  const { nextStep, prevStepData } = props;
  const [otpData, setOtpData] = useState<string>("");
  // Todo - later remove the initial value of integration data. This is for testing
  const [integrationData, setIntegrationData] = useState<IntegrationData | null>({
    integrationId: "abs-123-456-789",
    connectedAt: "11:49pm, Tue 21 Nov 2023"
  });

  const [tokenRegenratedCount, setTokenRegenratedCount] = useState<number>(0);

  // Todo - later remove the initial value of client key. This is for testing
  const [clientKey, setClientKey] = useState<string>("key");
  const [isGenerateOtpLoading, setGenerateOtpLoading] = useState(false);

  const [generateOtpError, setGenerateOtpError] = useState<GetDataError<Error> | string | null>(null);
  const [verifyConnectionError, setVerifyConnectionError] = useState<GetDataError<Error> | string | null>(null);
  const [createIntegrationError, setCreateIntegrationError] = useState<GetDataError<Error> | string | null>(null);

  const { copyToClipboard } = useCopyToClipboard();
  const { showError } = useToaster();

  useEffect(() => {
    // Start polling when otpData is true
    if (otpData && !clientKey) {
      // Polling every 5 seconds
      const intervalId = setInterval(() => {
        verifyConnection();
      }, 5000);

      // Cleanup interval on component unmount or when clientKey get available
      return () => clearInterval(intervalId);
    }
  }, [otpData, clientKey]);

  useEffect(() => {
    const error = generateOtpError || verifyConnectionError;
    if (error) {
      showError(getErrorMessage(error));
    }
  }, [generateOtpError, verifyConnectionError]);

  const handleGenerateOtp = async () => {
    try {
      const jiraToken = new JiraCloudGenerateTokenService();
      setGenerateOtpLoading(true);
      const response = await jiraToken.get();
      setOtpData(response.data);
      setTokenRegenratedCount(prevData => prevData + 1);
    } catch (error) {
      setGenerateOtpError((error as GetDataError<Error>)?.message || "An error occurred");
    } finally {
      setGenerateOtpLoading(false);
    }
  };

  const createJiraIntegration = async (clientKey: string) => {
    try {
      const jiraIntegration = new JiraCloudCreateIntegration();
      const jiraIntegrationPayload = getJiraIntegrationPayload(clientKey);
      const response = await jiraIntegration.create(jiraIntegrationPayload);
      setIntegrationData(response.data);
    } catch (error) {
      setCreateIntegrationError((error as GetDataError<Error>)?.message || "An error occurred");
    }
  };

  const verifyConnection = async () => {
    try {
      const verifyConnection = new JiraCloudVerifyConnectionService();
      const response = await verifyConnection.get(otpData);
      // Todo - later remove the default value of key. This is for testing
      const clientKey = response?.data || "key";
      if (response.status === OTP_NOT_SUBMITTED_STATUS_CODE) {
        setClientKey(clientKey);
        createJiraIntegration(clientKey);
      }
    } catch (error) {
      setVerifyConnectionError((error as GetDataError<Error>)?.message || "An error occurred");
    }
  };

  const renderConnectionStatus = () => {
    if (Boolean(!clientKey)) {
      return <VerifyingConnectionLoading />;
    } else if (integrationData) {
      return <IntegrationSuccess integrationData={integrationData} />;
    }
  };

  return (
    <Layout.Vertical className={css.configurationPanel}>
      <Text font={{ variation: FontVariation.H4 }}>{"Overview"}</Text>
      <Text
        font={{ variation: FontVariation.BODY2, size: "normal", weight: "light" }}
        color={Color.GREY_500}
        padding={{ top: "small", bottom: "small" }}>
        {"Provide basic overview information of your integration of easier management"}
      </Text>
      <Formik<JiraCloudConnectionDTO>
        initialValues={prevStepData ?? JIRA_CLOUD_CONNETION_INITIAL_VALUES}
        enableReinitialize={false}
        formName="jiraCloudConnection"
        onSubmit={data => {
          nextStep?.(data);
        }}
        validate={formData => vaidateCloudJiraConfigurations(formData)}>
        {formikProps => (
          <>
            <CardV2>
              <NameIdDescriptionTags
                formikProps={formikProps}
                className={css.jiraCloudManagedFields}
                identifierProps={{
                  inputLabel: "Integration Name",
                  isIdentifierEditable: false
                }}
                tooltipProps={{ dataTooltipId: "NameIdDescriptionTagsHealthSource" }}
                isIdentifierHidden
              />
            </CardV2>
            <Container>
              <Text font={{ variation: FontVariation.H4 }}>{"Install JIRA Connect App"}</Text>
              <Text
                font={{ variation: FontVariation.BODY2, size: "normal", weight: "light" }}
                color={Color.GREY_500}
                padding={{ top: "small", bottom: "small" }}>
                {"Follow these simple steps to connect Jira"}
              </Text>
              <CardV2>
                <Layout.Horizontal padding={{ bottom: "medium" }}>
                  <Container className={css.greyCircle}>1</Container>
                  <Text padding={{ left: "small" }} color={Color.BLACK}>
                    {"First, verify that you are an owner of the Jira account where you track issues"}
                  </Text>
                </Layout.Horizontal>
                <Layout.Horizontal padding={{ bottom: "medium" }}>
                  <Container className={css.greyCircle}>2</Container>
                  <Text padding={{ left: "small" }} color={Color.BLACK}>
                    {
                      "An easy way to check if your account is an collection owner is to visit your organisation page and verify the collection is listed."
                    }
                  </Text>
                </Layout.Horizontal>
                <Layout.Horizontal padding={{ bottom: "medium" }}>
                  <Container className={css.greyCircle}>3</Container>
                  <Layout.Vertical spacing={"small"}>
                    <Text padding={{ bottom: "small" }} color={Color.BLACK}>
                      {
                        "Go to the Atlassian Marketplace to install the app and configure the SEI app to access the Jira projects"
                      }
                    </Text>
                    <Button
                      variation={ButtonVariation.PRIMARY}
                      icon="service-jira"
                      type="submit"
                      text={"Install SEI App"}
                      iconProps={{ size: 12 }}
                      rightIcon="share"
                      className={css.configurationBtns}
                      target={"_blank"}
                      href={SEI_APP_URL}
                    />
                  </Layout.Vertical>
                </Layout.Horizontal>

                <Layout.Horizontal padding={{ bottom: "medium" }}>
                  <Container className={css.greyCircle}>4</Container>
                  <Layout.Vertical spacing={"small"}>
                    <Text padding={{ bottom: "small" }} color={Color.BLACK}>
                      {"Generate and copy the key when requested by the Jira connect app"}
                    </Text>
                    {otpData ? (
                      <>
                        <Text padding={{ top: "small", bottom: "xsmall" }} color={Color.GREY_700}>
                          {"Jira Connect App key"}
                        </Text>
                        <Layout.Horizontal>
                          <TextInput
                            disabled={true}
                            value={otpData}
                            leftIcon="gitops-gnupg-key-blue"
                            rightElement="code-copy"
                            rightElementProps={{
                              onClick: () => copyToClipboard(otpData),
                              size: 16,
                              padding: "small",
                              style: { cursor: "pointer" }
                            }}
                            leftIconProps={{ name: "gitops-gnupg-key-blue", size: 16 }}
                            className={css.otpDataTextInput}
                          />

                          <Button
                            variation={ButtonVariation.LINK}
                            iconProps={{ padding: { right: "small" } }}
                            icon="refresh"
                            type="submit"
                            text={"Regenerate key"}
                            className={css.configurationBtns}
                            onClick={handleGenerateOtp}
                          />
                        </Layout.Horizontal>
                      </>
                    ) : (
                      <Button
                        variation={ButtonVariation.SECONDARY}
                        icon="gitops-gnupg-key-blue"
                        type="submit"
                        text={"Generate Key"}
                        className={css.configurationBtns}
                        onClick={handleGenerateOtp}
                        loading={isGenerateOtpLoading}
                      />
                    )}
                  </Layout.Vertical>
                </Layout.Horizontal>
                {otpData ? (
                  <KeyExpirationTimer
                    keyExpirationTime={OTP_EXPIRATION_TIME}
                    tokenRegenratedCount={tokenRegenratedCount}
                  />
                ) : null}
              </CardV2>
              {otpData ? (
                <>
                  <Text font={{ variation: FontVariation.H4 }} padding={{ top: "xlarge", bottom: "large" }}>
                    {"Verify Connection"}
                  </Text>
                  <CardV2>{renderConnectionStatus()}</CardV2>
                </>
              ) : null}
            </Container>
            <Container>
              <Button
                variation={ButtonVariation.PRIMARY}
                type="submit"
                text={"Next: Select Jira Projects"}
                rightIcon="chevron-right"
                className={css.nextBtn}
                onClick={() => formikProps.submitForm()}
              />
            </Container>
          </>
        )}
      </Formik>
    </Layout.Vertical>
  );
}
