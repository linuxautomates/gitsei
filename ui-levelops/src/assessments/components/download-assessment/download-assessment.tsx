import React, { useCallback } from "react";
import { Document, Font, Page, Text, View } from "@react-pdf/renderer";
// import font from "assets/fonts/Inter-Regular.woff";
import { downloadAssessmentStyles as styles } from "./download-sheet.style";
import { getAssertion } from "assessments/utils/helper";
import { map } from "lodash";

interface DownloadAssessmentProps {
  assessment: any;
}

// Font.register({ family: "inter", src: font });

const DownloadAssessment: React.FC<DownloadAssessmentProps> = (props: DownloadAssessmentProps) => {
  const { assessment } = props;

  const getAnswer = useCallback(
    (sectionId: any, questionId: any) => getAssertion(assessment?.section_responses || [], sectionId, questionId),
    [assessment]
  );

  return (
    <Document>
      <Page style={styles.body}>
        <Text style={styles.title} fixed>
          {assessment?.qtemplate_id || ""}
        </Text>
        <Text style={styles.author} fixed>
          {assessment?.qtemplate_name || ""}
        </Text>
        {map(assessment?.section_responses || [], (section: any, index: number) => (
          <View style={styles.view}>
            <View style={styles.headerview}>
              <Text style={styles.subtitle}>
                Section {index + 1}: {section?.name || ""}
              </Text>
            </View>
            <Text style={styles.subtext}>{section?.description || ""}</Text>
            {map(section.questions, (question: any, qindex: number) => {
              const answer = getAnswer(section.id, question.id);
              return (
                <View style={styles.questionView}>
                  <Text style={styles.assertion}>
                    Question {qindex + 1}: {question?.name || ""}
                  </Text>
                  <Text style={styles.assertion}>
                    Answers:{" "}
                    {map(answer?.responses || [], val =>
                      val?.type === "file" ? val?.file_name || "File" : val?.value
                    ).join("\n ")}
                  </Text>
                  <Text style={styles.email}>Answered by: {answer?.user_email || ""}</Text>
                </View>
              );
            })}
          </View>
        ))}
        <Text
          style={styles.pageNumber}
          render={({ pageNumber, totalPages }) => `${pageNumber} / ${totalPages}`}
          fixed
        />
      </Page>
    </Document>
  );
};

export default React.memo(DownloadAssessment);
