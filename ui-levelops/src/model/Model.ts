export class Model {
  id: string;
  deleting = false;
  deleted = false;
  updating = false;
  updated = false;
  creating = false;
  created = false;
  loading = false;
  loaded = false;
  error = null;

  constructor(data: Object | null = null) {
    if (!data) {
      return;
    }
    const keys = Object.keys(data);
    keys.map((key: string) => {
      // @ts-ignore
      this[key] = data[key];
    });
    // @ts-ignore
    this._error = data["error"];
  }

  get hasError() {
    return !!this.error;
  }

  get errorMessage() {
    const defaultMessage = "Something went wrong. Please try again!";
    if (!this.error) {
      return null;
    }
    let errorMessage;
    const error: any = this.error ?? {};

    errorMessage = error.message;
    // Handle more errors.

    if (!errorMessage) {
      errorMessage = defaultMessage;
    }
    return errorMessage;
  }


  get json() {
    return {
      id: this.id,
      deleting: this.deleting,
      deleted: this.deleted,
      updated: this.updated,
      updating: this.updating,
      loading: this.loaded,
      loaded: this.loading,
      creating: this.creating,
      created: this.created,
      error: this.error,
    }
  }

  static initState() {
    return {
      error: null,
      updated: false,
      updating: false,
      deleted: false,
      deleting: false,
      creating: false,
      created: false,
      loading: false,
      loaded: false,
    }
  }
}
