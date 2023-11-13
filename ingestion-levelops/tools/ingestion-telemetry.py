#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import requests
import json
import sys
from time import sleep 
import argparse

## Use this command to port-forward the control plane
# kubectl port-forward deployment/ingestion-control-plane-deployment 8081:8081

URL="http://localhost:8081/control-plane/v1/agents"

def main():
  parser = argparse.ArgumentParser(description='Ingestion Telemetry')
  parser.add_argument('--watch', '-w', dest='watch', help='Number of seconds before refreshing the output', default=0)
  parser.add_argument('--tenant', '-t', dest='tenant', help='filter a specific tenant')
  parser.add_argument('--integration', '-i', dest='integration', help='filter a specific integration id')
  parser.add_argument('--agent', '-a', dest='agent', help='filter a specific agent')
  parser.add_argument('--logs', '-l', dest='logs', nargs='?', default='-1', help='display logs')
  parser.add_argument('--type', '-n', dest='type', nargs='?', help='Agent type')
  args = parser.parse_args()
  freq = int(args.watch)
  tenants = [ args.tenant ] if args.tenant else None
  integrations = [ args.integration ] if args.integration else None
  agents_filter = [ args.agent ] if args.agent else None
  logs = int(args.logs) if args.logs else 5
  agent_type = args.type if args.type else None

  try:
    while True:
      agents = get_agents()

      if freq > 0:
        print("\033[H\033[J") # clear screen
        print("Every {:.1f}s".format(freq))
      
      print_agents(agents, tenants, integrations, agents_filter, logs, agent_type)

      if freq <= 0:
        exit(0)

      sleep(freq)
  except KeyboardInterrupt:
      exit(0)

def get_agents():
    r = requests.get(URL)
    agents = r.json().get('records',[])
    return agents

def print_agents(agents, tenants=None, integrations=None, agents_filter=None, display_logs=False, agent_type=None):
    print(f'Total agents: {len(agents)}')
    filered_agents = []
    for agent in agents:
      tenant_id = agent.get('tenant_id', '-') 
      agent_id = agent.get('agent_id', '-')
      # print(agent)
      a_type = agent.get('agent_type', '-')
      if (tenants and tenant_id not in tenants) or (agents_filter and agent_id not in agents_filter) or (agent_type and a_type != agent_type):
        continue
      filered_agents.append(agent)

    print(f'filtered agents: {len(filered_agents)}\n\n')
    print (" {:^36} | {:^10} | {:^16} | {:^10} | {:^10} | {:^6} | {:^5} | {:^25} | {:^18} | {:^6} | {:^4}".format("AGENT ID", "TYPE", "TENANT", "INTEG", "VERSION", "#CTRLS", "CPU", "MEMORY", "JOBS", "UPTIME", "♥"))
    print (" {:36} | {:10} | {:16} | {:10} | {:10} | {:6} | {:5} | {:^13} | {:^9} | {:4} | {:4} | {:4} | {:^6} | {:4}".format("", "", "", "", "", "", "CORES", "USED", "MAX", "RUN.", "DONE", "BRKD", "", ""))
    hline = " {:-<36} | {:-<10} | {:-<16} | {:-<10} | {:-<10} | {:-<6} | {:-<5} | {:-<13} | {:-<9} | {:-<4} | {:-<4} | {:-<4} | {:-<6} | {:-<4}".format("", "", "", "", "", "", "", "", "", "", "", "", "", "")
    hline_flat = " {:-<36}---{:-<10}---{:-<16}---{:-<10}---{:-<10}---{:-<6}---{:-<5}---{:-<13}---{:-<9}---{:-<4}---{:-<4}---{:-<4}---{:-<6}---{:-<4}".format("", "", "", "", "", "", "", "", "", "", "", "", "", "")
    print (hline)
    for agent in filered_agents:
      tenant_id = agent.get('tenant_id', '-') 
      agent_id = agent.get('agent_id', '-')
      # print(agent)
      a_type = agent.get('agent_type', '-')
      id = agent.get('agent_id', '?')
      type = agent.get('agent_type', '?')
      controllers = agent.get('controller_names', [])
      last_heartbeat = agent.get('last_heartbeat', '?')
      last_heartbeat_since = agent.get('last_heartbeat_since', '?')

      integration_ids = agent.get('integration_ids', [])
      if integrations:
        if len([x for x in integration_ids if x in integrations]) == 0:
          continue

      integration_str = "-"
      if len(integration_ids) > 0:
        integration_str = ",".join(integration_ids)
      if len(integration_str) > 10:
        integration_str = integration_str[0:7] + "..."

      telemetry = agent.get('telemetry', {})
      version = telemetry.get('version', '?')
      uptime = telemetry.get('uptime', '?')
      if uptime != '?' and last_heartbeat_since != '?':
        uptime = uptime + last_heartbeat_since * 1000
      uptime_str = format_duration(uptime)

      mem_total = telemetry.get('memory', {}).get('total', '?')
      mem_max = telemetry.get('memory', {}).get('max', '?')
      mem_free = telemetry.get('memory', {}).get('free', '?')
      mem_used = mem_total - mem_free if mem_free != '?' and mem_total != '?' else '?'

      cpu_cores = telemetry.get('cpu', {}).get('cores', '?')

      jobs_total = telemetry.get('jobs', {}).get('total', '?')
      jobs_done = telemetry.get('jobs', {}).get('done', '?')
      jobs_running = telemetry.get('jobs', {}).get('running', '?')
      jobs_invalid_total = telemetry.get('jobs', {}).get('invalid_total', '?')
      jobs_invalid_not_done_but_not_running = telemetry.get('jobs', {}).get('invalid_not_done_but_not_running', '?')
      jobs_invalid_done_but_running = telemetry.get('jobs', {}).get('invalid_done_but_running', '?')

      log = telemetry.get('log', '')

      print (" {:35} | {:10} | {:16} | {:10} | {:10} | {:6} | {:>5} | {:>4} /{:>4} MB | {:>6} MB | {:>4} | {:>4} | {:>4} | {:>6} | {:>3}s".format(id, type, tenant_id, integration_str, version, len(controllers), cpu_cores, mem_used, mem_total, mem_max, jobs_running, jobs_done, jobs_invalid_total, uptime_str, last_heartbeat_since))

      if display_logs > 0 and len(log) > 0:
        print (hline_flat)
        indent = "   | ".format(" ")
        print (indent + ("\n" + indent).join(log.split('\n')[-(display_logs + 1):-1]))
        print (hline_flat)

def format_duration(duration_ms, max_char = 6, overflow = "..."):
  if not duration_ms or duration_ms == '?' or duration_ms <= 0:
    return '?'
  sec = duration_ms / 1000; minutes = sec / 60; hours = minutes / 60; days = hours / 24
  if int(days) > 0:
    return duration_to_str(days, "d", max_char, overflow)
  if int(hours) > 0:
    return duration_to_str(hours, "h", max_char, overflow)
  if int(minutes) > 0:
    return duration_to_str(minutes, "m", max_char, overflow)
  if int(sec) > 0:
    return duration_to_str(sec, "s", max_char, overflow)
  return duration_to_str(duration_ms, "ms", max_char - 1, overflow)

def duration_to_str(duration_f, timeunit, max_char = 6, overflow = "..."):
  string = "{:.1f}".format(duration_f)
  if string[-2:] == ".0":
    string = string[:-2]
  if len(string) > max_char - len(timeunit):
    return overflow
  return "{}{}".format(string, timeunit)

main()


