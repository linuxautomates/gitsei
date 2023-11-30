import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RestNotes } from "classes/RestNotes";
import LocalStoreService from "services/localStoreService";
import Loader from "../../components/Loader/Loader";
import { Comments } from "../../assessments/components";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { notesCreate, notesList } from "reduxConfigs/actions/restapi";
import { notesListState, notesCreateState } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";

import moment from "moment";

interface NotesContainerProps {
  workItemid: string;
}

const NotesContainer: React.FC<NotesContainerProps> = props => {
  const dispatch = useDispatch();
  const notesState = {
    list: useSelector(state => notesListState(state)),
    create: useSelector(state => notesCreateState(state))
  };

  const [loading, setLoading] = useState(false);
  const [workItemid, setWorkItemId] = useState<null | string>(null);
  const [new_comment, setNewComment] = useState<any>({});
  const [notes_create_loading, setNotesCreateLoading] = useState(false);
  const [loading_due_to_create, setLoadingDueToCreate] = useState(false);
  const [notes, setNotes] = useState<any[]>([]);
  const [create_comment_failed_at, setCreateCommentFailedAt] = useState<Number | null>(null);
  const [create_comment_id, setCreateCommentId] = useState<null | string>(null);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("notes", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    if (props.workItemid !== workItemid) {
      dispatch(restapiClear("notes", "list", "0"));
      dispatch(notesList({ filter: { work_item_id: props.workItemid } }));
      setLoading(true);
      setWorkItemId(props.workItemid);
    }
  }, [props]);

  useEffect(() => {
    if (notes_create_loading) {
      const { loading: _loading, error } = get(notesState, ["create", create_comment_id || "0"], {
        loading: true,
        error: true
      });
      if (!_loading) {
        if (error) {
          setNotesCreateLoading(false);
          setCreateCommentFailedAt(moment().unix());
        } else {
          dispatch(restapiClear("notes", "list", "0"));
          // @ts-ignore
          dispatch(restapiClear("notes", "create", create_comment_id));
          dispatch(notesList({ filter: { work_item_id: props.workItemid } }));

          setLoadingDueToCreate(true);
          setNotesCreateLoading(false);
          setCreateCommentFailedAt(null);
          setNewComment({});
        }
      }
    }
  }, [notesState]);

  useEffect(() => {
    if (loading || loading_due_to_create) {
      const { loading: _loading, error } = get(notesState, ["list"], { loading: true, error: true });
      if (!_loading && _loading !== undefined) {
        if (!error && error !== undefined) {
          const notes = get(notesState, ["list"], {}).data?.records;
          setNotes(notes);
        }
        setLoadingDueToCreate(false);
        setLoading(false);
      }
    }
  }, [notesState]);

  useEffect(() => {
    if (Object.keys(new_comment).length > 0) {
      handleNoteCreate();
    }
  }, [new_comment]);

  const handleNoteCreate = () => {
    const note = new RestNotes();
    const ls = new LocalStoreService();

    const noteId = props.workItemid + "-" + moment.utc();

    note.body = new_comment.body;
    note.creator = ls.getUserEmail();
    note.work_item_id = props.workItemid;

    setNotesCreateLoading(true);
    setCreateCommentId(noteId);

    dispatch(notesCreate(note, noteId));
  };

  const getNotes = () => {
    if (loading) {
      return <Loader />;
    }

    const ls = new LocalStoreService();
    const creator = ls.getUserEmail();

    return (
      <Comments
        comments={notes}
        commentKey="body"
        creatorKey="creator"
        creator={creator}
        onAddComment={(comment: any) => {
          setNewComment(comment);
        }}
        commentCreateDisabled={notes_create_loading}
        commentCreateFailedAt={create_comment_failed_at}
        commentCreateText={create_comment_failed_at && new_comment.createCommentText}
        createCommentOnTop={true}
      />
    );
  };

  return <>{getNotes()}</>;
};

export default NotesContainer;
