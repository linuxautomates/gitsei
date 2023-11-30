import React from "react";
import * as PropTypes from "prop-types";
import { Document, Font, Page, StyleSheet, Text, View } from "@react-pdf/renderer";
import font from "assets/fonts/Inter-Regular.woff";

Font.register({ family: "inter", src: font });
const styles = StyleSheet.create({
  body: {
    paddingTop: 35,
    paddingBottom: 65,
    paddingHorizontal: 35
  },
  title: {
    fontSize: 10,
    textAlign: "center",
    fontFamily: "inter"
  },
  author: {
    fontSize: 18,
    textAlign: "center",
    marginBottom: 20,
    marginTop: 30,
    fontFamily: "inter"
  },
  subtitle: {
    fontSize: 14,
    lineHeight: 1.4,
    fontFamily: "inter"
  },
  text: {
    margin: 12,
    fontSize: 14,
    textAlign: "justify",
    fontFamily: "inter"
  },
  assertion: {
    marginLeft: 12,
    marginRight: 12,
    marginBottom: 8,
    fontSize: 10,
    lineHeight: 1.4,
    //textAlign: "justify",
    fontFamily: "inter"
  },
  email: {
    marginLeft: 12,
    marginRight: 12,
    marginBottom: 8,
    fontSize: 10,
    textAlign: "justify",
    color: "#2967dd",
    fontFamily: "inter"
  },
  subtext: {
    margin: 12,
    marginTop: 8,
    fontSize: 10,
    textAlign: "justify",
    fontFamily: "inter",
    color: "grey"
  },
  image: {
    marginVertical: 15,
    marginHorizontal: 100
  },
  header: {
    fontSize: 12,
    marginBottom: 20,
    textAlign: "center",
    color: "grey",
    fontFamily: "inter"
  },
  pageNumber: {
    position: "absolute",
    fontSize: 12,
    bottom: 30,
    left: 0,
    right: 0,
    textAlign: "center",
    color: "grey",
    fontFamily: "inter"
  },
  view: {
    marginBottom: 20
  },
  questionView: {
    marginTop: 10,
    marginBottom: 10,
    borderBottom: "1pt solid #dcdfe4"
  },
  headerview: {
    border: "1pt solid #828282",
    borderRadius: 4,
    justifyContent: "flex-start",
    alignItems: "flex-start",
    paddingVertical: 10,
    paddingLeft: 12
  }
});

export class DownloadAssessmentComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.getAnswer = this.getAnswer.bind(this);
  }

  getAnswer(sectionId, questionId) {
    const quiz = this.props.assessment;
    let retAssertion = undefined;
    quiz.section_responses.forEach(answer => {
      if (answer.section_id.toString() === sectionId.toString()) {
        answer.answers.forEach(question => {
          if (question.question_id.toString() === questionId.toString()) {
            if (question.answered) {
              retAssertion = question;
            }
          }
        });
      }
    });
    return retAssertion;
  }

  render() {
    const { assessment } = this.props;
    return (
      <Document>
        <Page style={styles.body}>
          <Text style={styles.title} fixed>
            {assessment.qtemplate_id}
          </Text>
          <Text style={styles.author} fixed>
            {assessment.qtemplate_name}
          </Text>
          {assessment.sections.map((section, index) => (
            <View style={styles.view}>
              <View style={styles.headerview}>
                <Text style={styles.subtitle}>
                  Section {index + 1}: {section.name}
                </Text>
              </View>
              <Text style={styles.subtext}>{section.description}</Text>
              {/*eslint-disable-next-line array-callback-return*/}
              {section.questions.map((question, qindex) => {
                const answer = this.getAnswer(section.id, question.id);
                if (answer && answer.responses.length > 0) {
                  return (
                    <View style={styles.questionView}>
                      <Text style={styles.assertion}>
                        Question {qindex + 1}: {question.name}
                      </Text>
                      <Text style={styles.assertion}>
                        Answers:{" "}
                        {answer.responses.map(val => (val.type === "file" ? val.file_name : val.value)).join("\n ")}
                      </Text>
                      {/*{answer.responses[0].type === "file" && (*/}
                      {/*  <Text style={styles.assertion}>File: {answer.responses[0].file_name}</Text>*/}
                      {/*)}*/}
                      {/*{answer.responses[0].type !== "file" && (*/}
                      {/*  <Text style={styles.assertion}>*/}
                      {/*    Answers: {answer.responses.map(val => val.value).join(", ")}*/}
                      {/*  </Text>*/}
                      {/*)}*/}
                      <Text style={styles.email}>Answered by: {answer.user_email}</Text>
                    </View>
                  );
                }
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
  }
}

DownloadAssessmentComponent.propTypes = {
  assessment: PropTypes.object.isRequired
};
