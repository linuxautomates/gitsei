export class RestSignatureLog {
  constructor(restData = null) {
    this._id = undefined;
    this._results = {};
    this._labels = [];
    this._metadata = {};
    this._product_id = undefined;
    this._signature_id = undefined;
    this._success = undefined;
    this._timestamp = undefined;

    if (restData) {
      this._id = restData?.id;
      this._results = restData?.results;
      this._labels = restData?.labels;
      this._metadata = restData?.metadata;
      this._product_id = restData?.product_id;
      this._signature_id = restData?.signature_id;
      this._success = restData?.success;
      this._timestamp = restData?.timestamp;
    }
  }

  get id() {
    return this._id;
  }

  get results() {
    return this._results;
  }

  get labels() {
    return this._labels;
  }

  get metadata() {
    return this._metadata;
  }

  get product_id() {
    return this._product_id;
  }

  get signature_id() {
    return this._signature_id;
  }

  get success() {
    return this._success;
  }

  get timestamp() {
    return this._timestamp;
  }
}
