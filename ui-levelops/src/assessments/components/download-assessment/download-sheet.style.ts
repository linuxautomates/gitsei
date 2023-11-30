import { StyleSheet } from "@react-pdf/renderer";

export const downloadAssessmentStyles = StyleSheet.create({
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
