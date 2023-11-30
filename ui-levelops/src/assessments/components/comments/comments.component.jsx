import React from "react";
import * as PropTypes from "prop-types";
import { Col, Comment, Row, Tooltip, Typography } from "antd";
import { NameAvatar } from "../../../shared-resources/components";
import "../questions/questions.style.scss";
import { AntButton } from "shared-resources/components";
import { REGEX_COMMENTS_EMAIL, REGEX_COMMENTS_MENTION } from "utils/regexUtils";
import { GenericMentions } from "shared-resources/containers";
import moment from "moment";

const { Text } = Typography;

const LIST_KEY = "mentions_search";

class CommentsComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      current_comment: "",
      comment_create_failed_at: null
    };

    this.handleAddComment = this.handleAddComment.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (
      props.commentCreateFailedAt !== null &&
      props.commentCreateFailedAt > state.comment_create_failed_at &&
      state.current_comment !== props.commentCreateText
    ) {
      return {
        current_comment: props.commentCreateText,
        comment_create_failed_at: props.commentCreateFailedAt
      };
    }
  }

  getFormattedComment(str) {
    return str.split(REGEX_COMMENTS_MENTION).map(text => {
      if (text.match(REGEX_COMMENTS_EMAIL)) {
        // Handling mention
        return (
          <span
            className="email-format pr-5 pl-5"
            style={{
              color: "var(--blue6)",
              fontWeight: "bold"
            }}>
            {
              // Stripping @[] from mention
              text.replace(REGEX_COMMENTS_EMAIL, "$1")
            }
          </span>
        );
      }

      // Returning plain-text which is
      // plain text wrapped around mentions
      return text;
    });
  }

  get comments() {
    const { comments } = this.props;
    return (
      <Row type={"flex"} justify={"start"}>
        {comments.map((comment, index) => (
          <Col span={24}>
            <Comment
              key={`note-${index}`}
              actions={[]}
              author={comment[this.props.creatorKey]}
              avatar={<NameAvatar name={comment[this.props.creatorKey]} />}
              content={
                <Text style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}>
                  {this.getFormattedComment(comment[this.props.commentKey])}
                </Text>
              }
              datetime={
                <Tooltip title={moment().format("YYYY-MM-DD HH:mm:ss")}>
                  <span>{moment.unix(comment.created_at).fromNow()}</span>
                </Tooltip>
              }
            />
          </Col>
        ))}
      </Row>
    );
  }

  handleAddComment() {
    // Saving only if non-empty string
    if (this.state.current_comment.trim()) {
      const { onAddComment, creator } = this.props;
      const newComment = {
        [this.props.creatorKey]: creator,
        created_at: Math.floor(Date.now()) / 1000,
        [this.props.commentKey]: this.state.current_comment
      };
      this.setState({ current_comment: "" }, () => onAddComment(newComment));
    }
  }

  get addComment() {
    const { creator, commentCreateDisabled } = this.props;
    return (
      <Row gutter={[10, 10]} style={{ paddingTop: "10px" }} align={"center"} type={"flex"}>
        <Col span={1} className="pr-5">
          <NameAvatar name={creator} />
        </Col>
        <Col span={18}>
          <GenericMentions
            placeholder="Add a comment...."
            value={this.state.current_comment}
            id={LIST_KEY}
            uri="users"
            method="list"
            searchField="email"
            onChange={comment => {
              this.setState({ current_comment: comment });
            }}
            disabled={commentCreateDisabled}
            optionRenderer={(user, email) => {
              return (
                <>
                  <NameAvatar name={email} />
                  {email}
                </>
              );
            }}
            optionValueTransformer={option => `[${option.email}]`}
            loadOnMount={false}
          />
        </Col>
        <Col span={5}>
          <AntButton onClick={this.handleAddComment} type={"primary"} disabled={commentCreateDisabled}>
            Add Comment
          </AntButton>
        </Col>
      </Row>
    );
  }

  render() {
    const { createCommentOnTop } = this.props;

    return (
      <div
        className={"comments"}
        style={{
          display: "flex",
          flexDirection: createCommentOnTop ? "column-reverse" : "column"
        }}>
        <div style={{ width: "70%" }}>{this.comments}</div>
        {this.addComment}
      </div>
    );
  }
}

CommentsComponent.propTypes = {
  comments: PropTypes.array.isRequired,
  commentKey: PropTypes.string,
  creatorKey: PropTypes.string,
  onAddComment: PropTypes.func.isRequired,
  creator: PropTypes.string.isRequired,
  commentCreateDisabled: PropTypes.bool.isRequired,
  commentCreateText: PropTypes.string.isRequired,
  commentCreateFailedAt: PropTypes.number,
  createCommentOnTop: PropTypes.bool // If we want to show create comment above comment list
};

CommentsComponent.defaultProps = {
  comments: [],
  commentKey: "message",
  creatorKey: "user",
  commentCreateDisabled: false,
  commentCreateText: "",
  commentCreateFailedAt: null,
  createCommentOnTop: false
};

export default CommentsComponent;
