import {buildLink} from "../integrationUtils"
import { removeEmptyIntegrations } from "../integrationUtils";

describe("Remove Empty Integrations tests",()=>{
    
    it('For undefined, null, and empty string values', () => {
        const data = {
          key1: 'abc',
          key2: undefined,
          key3: null,
          key4: ''
        };
        const result = removeEmptyIntegrations(data);
    
        expect(result).toEqual({
          key1: 'abc'
        });
      });
    
      it('For handling "false" and "0" values correctly', () => {
        const data = {
          key1: 'abc',
          key2: false,
          key3: 0,
          key4: 'def',
        };
        const result = removeEmptyIntegrations(data);
    
        expect(result).toEqual({
          key1: 'abc',
          key2: false,
          key3: 0,
          key4: 'def',
        });
      });
    
      it('handles an empty input object', () => {
        const data = {};
        const result = removeEmptyIntegrations(data);
    
        expect(result).toEqual({});
      });
})

describe("Integration Utils tests",()=>{
    it('should build a Jira link', () => {
        const item = 'JIRA-1';
        const url = 'https://jira.example.com';
        const application = 'jira';
        const result = buildLink(item, url, application);
        expect(result).toBe('https://jira.example.com/browse/JIRA-1');
      });
    
      it('should build a GitHub link', () => {
        const item = {
          repo_cloud_id: 'repo123',
          commit_sha: 'abc123',
        };
        const url = 'https://github.com';
        const application = 'github';
        const result = buildLink(item, url, application);
        expect(result).toBe('https://github.com/repo123/commit/abc123');
      });
    
      it('should handle an unknown application and return undefined', () => {
        const item = 'JIRA-2';
        const url = 'https://jira.example.com';
        const application = 'unknown-app';
        const result = buildLink(item, url, application);
        expect(result).toBeUndefined();
      });

})