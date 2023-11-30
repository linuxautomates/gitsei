export class RestTeams {
    constructor() {
        this._name = undefined;
        this._organizationId = undefined;
        this.json = this.json.bind(this);
    }

    get name() {
        return this._name;
    }

    set name(nm) {
        this._name = nm;
    }

    get organizationId() {
        return this._organizationId;
    }

    set organizationId(org) {
        this._organizationId = org;
    }

    json() {
        return (
            {
                organization_id: this._organizationId,
                name: this._name
            }
        );
    }

    static validate(data) {
        let result = true;
        result = result && data.hasOwnProperty("name");
        result = result && data.hasOwnProperty("organization_id");
        return result;
    }
}