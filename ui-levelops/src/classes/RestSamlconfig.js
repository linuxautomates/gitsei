export class RestSamlconfig {
  constructor(restData) {
    this._spId = undefined;
    this._acsUrl = undefined;
    this._enabled = false;
    this._idpId = undefined;
    this._idpSsoUrl = undefined;
    this._idpCert = undefined;
    this._defaultRelayState = undefined;
    if (restData) {
      this._spId = restData.sp_id;
      this._acsUrl = restData.acs_url;
      this._enabled = restData.enabled === undefined ? false : restData.enabled;
      this._idpId = restData.idp_id;
      this._idpSsoUrl = restData.idp_sso_url;
      this._defaultRelayState = restData.default_relay_state;
      this._idpCert = restData.idp_cert === undefined ? "" : atob(restData.idp_cert);
    }
    this.json = this.json.bind(this);
  }

  get spId() {
    return this._spId;
  }

  set spId(id) {
    this._spId = id;
  }

  get acsUrl() {
    return this._acsUrl;
  }

  set acsUrl(url) {
    this._acsUrl = url;
  }

  get enabled() {
    return this._enabled;
  }

  set enabled(en) {
    this._enabled = en;
  }

  get idpId() {
    return this._idpId;
  }

  set idpId(id) {
    this._idpId = id;
  }

  get idpSsoUrl() {
    return this._idpSsoUrl;
  }

  set idpSsoUrl(url) {
    this._idpSsoUrl = url;
  }

  get idpCert() {
    return this._idpCert;
  }

  get defaultRelayState() {
    return this._defaultRelayState;
  }

  set defaultRelayState(defaultRelayState) {
    return (this._defaultRelayState = defaultRelayState);
  }

  set idpCert(cert) {
    this._idpCert = cert;
  }

  json() {
    return {
      enabled: this._enabled,
      idp_id: this._idpId,
      idp_sso_url: this._idpSsoUrl,
      default_relay_state: this.defaultRelayState,
      idp_cert: btoa(this._idpCert),
      sp_id: this._spId,
      acs_url: this._acsUrl
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("sp_id");
    valid = valid && data.hasOwnProperty("acs_url");
    if (data.hasOwnProperty("enabled")) {
      valid = valid && data.hasOwnProperty("idp_id");
      valid = valid && data.hasOwnProperty("idp_sso_url");
      valid = valid && data.hasOwnProperty("idp_cert");
    }
    return valid;
  }
}
