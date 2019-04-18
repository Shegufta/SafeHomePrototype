import random
import numpy as np
from statistics import mean, median, stdev
import matplotlib.pyplot as plt
from time import sleep

NUM_RUN = 2000
TTL_NUM_DEV = 10          # total number of device
NUM_RTN = 100             # total number of routines
MAX_NUM_CMD_PER_RTN = 6   # max number of command per routine (min is 2 in this simulation)
CONCURRENT_LEVEL = 3      # run two concurrent routines together
# EXECTUION_TIME = [5, 10, 15, 20, 25]
# EXEC_TIME_DIST = [0.2, 0.2, 0.2, 0.2, 0.2]
EXECTUION_TIME = [5, 10, 15, 20, 1000]
EXEC_TIME_DIST = [0.8, 0.102, 0.06, 0.029, 0.009]
MAX_CMD_EXEC_TIME = max(EXECTUION_TIME)  # longest duration of a command (especially for LRR)
LRR_REQUIRED = True

ABORT_TIMEOUT = 15


class Routine:
  def __init__(self, dev_list, sta_list, dur_list, rid = -1):
    self.dev_list = dev_list
    self.sta_list = sta_list
    self.dur_list = dur_list
    self.rid = rid

  def print_rtn(self):
    print "\nRoutine rid {0}:".format(self.rid)
    for i in xrange(len(self.dev_list)):
      print "    Dev: {0} with target state: {1} at time {2}.".format(self.dev_list[i], self.sta_list[i], self.dur_list[i])

def getOneRandomRtn(num_cmd, rid = -1):
  dev_list = random.sample(range(0, TTL_NUM_DEV), num_cmd)
  sta_list = [0] * num_cmd
  dur_list = np.random.choice(EXECTUION_TIME, num_cmd, p=EXEC_TIME_DIST).tolist()
  # dur_list = [10] * num_cmd
  return Routine(dev_list, sta_list, dur_list, rid)

def generateNRandomRtn(num_rtn):
  routine_bank = []
  # Generate routines
  for i in xrange(num_rtn):
    num_cmd = random.randint(2, MAX_NUM_CMD_PER_RTN)
    routine_bank.append(getOneRandomRtn(num_cmd, i+1))
    # routine_bank[i].print_rtn()

  return routine_bank

def generateCCVsDevRtn():
  routine_bank = []
  sta_list = [0] * 3
  dur_list = [10] * 3
  routine_bank.append(Routine([1, 2, 3], sta_list, dur_list, 1))
  routine_bank.append(Routine([1, 4, 5], sta_list, dur_list, 2))
  routine_bank.append(Routine([2, 4, 6], sta_list, dur_list, 3))
  return routine_bank

def generateMultiAccessToOneDev():
  routine_bank = []
  sta_list = [0] * 3
  dur_list = [3] * 3
  routine_bank.append(Routine([1, 2, 3], sta_list, dur_list, 1))
  routine_bank.append(Routine([1, 4, 5], sta_list, dur_list, 2))
  routine_bank.append(Routine([1, 4, 6], sta_list, dur_list, 3))
  return routine_bank


def pickConcurrentRtn(rtn_bank, num_picked, lrr_required = False):
  if lrr_required:
    rtn_bank_LRR = [rtn for rtn in rtn_bank if max(rtn.dur_list) > MAX_CMD_EXEC_TIME - 1]
    if len(rtn_bank_LRR) == 0:
      return random.sample(rtn_bank, num_picked)
    rtn_bank_SRR = list(set(rtn_bank) - set(rtn_bank_LRR))
    num_LRR = max(1, num_picked * 1.0 * len(rtn_bank_LRR) / len(rtn_bank))
    num_SRR = num_picked - num_LRR
    samples = random.sample(rtn_bank_LRR, num_LRR) + random.sample(rtn_bank_SRR, num_SRR)
    random.shuffle(samples)
    return samples
  else:
    return random.sample(rtn_bank, num_picked)

def runSequentially(rtns):
  t_total = 0
  for routine in rtns:
    t_total += sum(routine.dur_list)

  return t_total

def runWithCCLock(rtns):
  n_total = len(rtns)
  t_total = 0
  num_finished = 0
  t_max_running = 0
  running_rtns = []
  running_idxs = []
  running_devs = set([])
  while num_finished < n_total:
    for i, routine in enumerate(rtns):
      if len(running_devs.intersection(set(routine.dev_list))) == 0:
        num_finished += 1
        running_devs |= set(routine.dev_list)
        running_idxs.append(i)
        t_max_running = max([t_max_running] + routine.dur_list)
    # print "\n One Round with index {0} for time {1} with finished routine number {2}".format(running_idxs, t_max_running, num_finished)
    rtns = [i for j, i in enumerate(rtns) if j not in running_idxs]
    # print "Updated rtn size {0}".format(len(rtns))
    running_devs.clear()
    running_idxs = []
    t_total += t_max_running
    t_max_running = 0

  return t_total

def runWithDevLock(rtns):
  t_total = 0
  dev_locks = [0] * TTL_NUM_DEV
  for routine in rtns:
    for i in xrange(len(routine.dev_list)):
      dev_locks[routine.dev_list[i]] += routine.dur_list[i]

  return max(dev_locks)

def runSeqInsideRtnWithDevLock(rtns):
  # inside routine: sequential, across routine: parallel
  # TODO: This might has optimization: how to arrange these routines. Not considered for now.
  t_total = 0
  rtn_locks = np.array([0] * len(rtns))
  dev_locks = np.array([0] * TTL_NUM_DEV)
  dev_cur_cmd_finish_time = np.array([0] * TTL_NUM_DEV)
  num_cmds_ttl_per_rtn = [len(routine.dev_list) for routine in rtns]
  num_cmds_left_per_rtn = np.array(num_cmds_ttl_per_rtn[:])
  cur_dev_per_rtn = np.array([-1] * len(rtns))
  nxt_cmd_per_rtn = [[rtn.dev_list[0], rtn.sta_list[0], rtn.dur_list[0]] for rtn in rtns]

  while (np.count_nonzero(num_cmds_left_per_rtn) > 0 or np.count_nonzero(dev_cur_cmd_finish_time) > 0):
    # Prepare all non-conflict commands in queue.
    for i, cmd in enumerate(nxt_cmd_per_rtn):
      if rtn_locks[i] == 1 or num_cmds_left_per_rtn[i] == 0:
        continue

      if dev_locks[cmd[0]] == 0:  # the device is available to execute
        dev_locks[cmd[0]] = 1     # lock the device
        rtn_locks[i] = 1          # lock the routine
        cur_dev_per_rtn[i] = cmd[0] # dev cmd[0] is running for routine [i]
        dev_cur_cmd_finish_time[cmd[0]] = cmd[2]
        # print "Executing Routine {0}'s {1}-th cmd on Dev {2} for {3}".format(i, num_cmds_ttl_per_rtn[i] - num_cmds_left_per_rtn[i] + 1, cmd[0], cmd[2])
        # push a new cmd to the head of the queue
        num_cmds_left_per_rtn[i] -= 1
        if num_cmds_left_per_rtn[i] == 0:
          # print "Routine {0}-th is finishing".format(i) 
          continue
        cmd_idx = num_cmds_ttl_per_rtn[i] - num_cmds_left_per_rtn[i]
        # print num_cmds_ttl_per_rtn, num_cmds_left_per_rtn, i, cmd_idx, dev_locks
        nxt_cmd_per_rtn[i] = [rtns[i].dev_list[cmd_idx], rtns[i].sta_list[cmd_idx], rtns[i].dur_list[cmd_idx]]
        # print "Next cmd: {0}".format(nxt_cmd_per_rtn)
      else:
        # print "Queueing  Routine {0}'s {1}-th cmd on Dev {2} for {3}".format(i, num_cmds_ttl_per_rtn[i] - num_cmds_left_per_rtn[i] + 1, cmd[0], cmd[2])
        continue

    # Finish one cmd
    t_min_exec = np.min(dev_cur_cmd_finish_time[np.nonzero(dev_cur_cmd_finish_time)])
    t_total += t_min_exec
    for dev in np.where(dev_cur_cmd_finish_time == t_min_exec)[0]:
      rtn_locks[np.where(cur_dev_per_rtn == dev)] = 0
      # print "Routine {0} releases Device {1} at time {2}".format(np.where(cur_dev_per_rtn == dev)[0], dev, t_total)
    # print "Routine lock: {0}".format(rtn_locks)
    
    dev_locks[np.where(dev_cur_cmd_finish_time == t_min_exec)[0]] = 0  # free up finished devices
    dev_cur_cmd_finish_time[np.where(dev_cur_cmd_finish_time > 0)[0]] -= t_min_exec
    # print "Still running devices: {0}\n--------------".format(dev_cur_cmd_finish_time)

  return t_total

def runSeqInsideRtnWithDevLockAndAbort(rtns, timeout=10):
  # inside routine: sequential, across routine: parallel
  # TODO: This might has optimization: how to arrange these routines. Not considered for now.
  t_total = 0
  num_rtn_aborted = 0
  rtn_locks = np.array([0] * len(rtns))
  dev_locks = np.array([0] * TTL_NUM_DEV)
  dev_cur_cmd_finish_time = np.array([0] * TTL_NUM_DEV)
  num_cmds_ttl_per_rtn = [len(routine.dev_list) for routine in rtns]
  num_cmds_left_per_rtn = np.array(num_cmds_ttl_per_rtn[:])
  cur_dev_per_rtn = np.array([-1] * len(rtns))
  nxt_cmd_per_rtn = [[rtn.dev_list[0], rtn.sta_list[0], rtn.dur_list[0]] for rtn in rtns]

  rtn_waiting_flag = np.array([0] * len(rtns))
  rtn_start_waiting_time = np.array([0] * len(rtns))

  while (np.count_nonzero(num_cmds_left_per_rtn) > 0 or np.count_nonzero(dev_cur_cmd_finish_time) > 0):
    # Prepare all non-conflict commands in queue.
    for i, cmd in enumerate(nxt_cmd_per_rtn):
      if rtn_locks[i] == 1 or num_cmds_left_per_rtn[i] == 0:
        continue


      if dev_locks[cmd[0]] == 0:  # the device is available to execute
        dev_locks[cmd[0]] = 1     # lock the device
        rtn_locks[i] = 1          # lock the routine
        cur_dev_per_rtn[i] = cmd[0] # dev cmd[0] is running for routine [i]
        dev_cur_cmd_finish_time[cmd[0]] = cmd[2]
        # print "Executing Routine {0}'s {1}-th cmd on Dev {2} for {3}".format(i, num_cmds_ttl_per_rtn[i] - num_cmds_left_per_rtn[i] + 1, cmd[0], cmd[2])
        # push a new cmd to the head of the queue
        num_cmds_left_per_rtn[i] -= 1
        if num_cmds_left_per_rtn[i] == 0:
          # print "Routine {0}-th is finishing".format(i) 
          continue
        cmd_idx = num_cmds_ttl_per_rtn[i] - num_cmds_left_per_rtn[i]
        # print num_cmds_ttl_per_rtn, num_cmds_left_per_rtn, i, cmd_idx, dev_locks
        nxt_cmd_per_rtn[i] = [rtns[i].dev_list[cmd_idx], rtns[i].sta_list[cmd_idx], rtns[i].dur_list[cmd_idx]]
        # print "Next cmd: {0}".format(nxt_cmd_per_rtn)
      elif dev_cur_cmd_finish_time[cmd[0]] > timeout or \
      (rtn_waiting_flag[i] > 0 and t_total + dev_cur_cmd_finish_time[cmd[0]] - rtn_start_waiting_time[i] > timeout):
        num_rtn_aborted += 1
        # clean up that routine
        num_cmds_left_per_rtn[i] = 0
        # print "------ Aborting Routine {0} --------".format(i)
        continue
      elif rtn_waiting_flag[i] == 0:
        # print "Queueing  Routine {0}'s {1}-th cmd on Dev {2} for {3}".format(i, num_cmds_ttl_per_rtn[i] - num_cmds_left_per_rtn[i] + 1, cmd[0], cmd[2])
        rtn_start_waiting_time[i] = t_total
        rtn_waiting_flag[i] = 1
        continue

    # Finish one cmd
    t_min_exec = np.min(dev_cur_cmd_finish_time[np.nonzero(dev_cur_cmd_finish_time)])
    t_total += t_min_exec
    for dev in np.where(dev_cur_cmd_finish_time == t_min_exec)[0]:
      rtn_locks[np.where(cur_dev_per_rtn == dev)] = 0
      # print "Routine {0} releases Device {1} at time {2}".format(np.where(cur_dev_per_rtn == dev)[0], dev, t_total)
    # print "Routine lock: {0}".format(rtn_locks)
    
    dev_locks[np.where(dev_cur_cmd_finish_time == t_min_exec)[0]] = 0  # free up finished devices
    dev_cur_cmd_finish_time[np.where(dev_cur_cmd_finish_time > 0)[0]] -= t_min_exec
    # print "Still running devices: {0}\n--------------".format(dev_cur_cmd_finish_time)

  return t_total, num_rtn_aborted * 1.0 /len(rtns)

def runSeqInsideRtnWithCCLock(rtns):
  n_total = len(rtns)
  t_total = 0
  num_scheduled = 0
  running_idxs = []
  dev_locks = np.array([0] * TTL_NUM_DEV)
  dev_cur_rtn_finish_time = np.array([0] * TTL_NUM_DEV)
  
  while (num_scheduled < n_total or np.count_nonzero(dev_cur_rtn_finish_time) > 0):
    # Finish (at least) one running routine.
    if (np.count_nonzero(dev_cur_rtn_finish_time) > 0):
      t_min_exec = np.min(dev_cur_rtn_finish_time[np.nonzero(dev_cur_rtn_finish_time)])
      t_total += t_min_exec
      # release the finished routine's dev lock
      dev_locks[np.where(dev_cur_rtn_finish_time == t_min_exec)[0]] = 0
      dev_cur_rtn_finish_time[np.where(dev_cur_rtn_finish_time > 0)[0]] -= t_min_exec
    
    # Deploy all runnable routines with the finished routines released
    for i, routine in enumerate(rtns):
      running_devs = set([idx for idx, dev_stat in enumerate(dev_locks) if dev_stat != 0])
      if len(running_devs.intersection(set(routine.dev_list))) == 0:  # The routine is able to run 
        # print "Routine {0} on dev {1} with running time {2} is scheduled at time {3}.".format(
        #   routine.rid, routine.dev_list, sum(routine.dur_list), t_total)
        num_scheduled += 1
        running_devs |= set(routine.dev_list)
        running_idxs.append(i)
        # Set the lock for new running routine. lock finish time: routine finsh time
        dev_locks[routine.dev_list] = 1
        dev_cur_rtn_finish_time[routine.dev_list] = sum(routine.dur_list)

    # rtns stores the un-scheduled routiens
    rtns = [i for j, i in enumerate(rtns) if j not in running_idxs]
    # print "Updated rtn size {0} with num_scheduled = {1} at time {2}".format(len(rtns), num_scheduled, t_total)
    # sleep(1)
    running_idxs = []
  return t_total

def runSeqInsideRtnWithCCLockAndAbort(rtns, timeout=10):
  # In this design, all routines requires the lock before ABORT_TIMEOUT, which can be decided at the very beginning.

  n_total = len(rtns)
  t_total = 0
  num_scheduled = 0
  num_rtn_aborted = 0
  running_idxs = []
  dev_locks = np.array([0] * TTL_NUM_DEV)
  dev_cur_rtn_finish_time = np.array([0] * TTL_NUM_DEV)
  
  while (num_scheduled < n_total or np.count_nonzero(dev_cur_rtn_finish_time) > 0):
    # Finish (at least) one running routine.
    if (np.count_nonzero(dev_cur_rtn_finish_time) > 0):
      t_min_exec = np.min(dev_cur_rtn_finish_time[np.nonzero(dev_cur_rtn_finish_time)])
      t_total += t_min_exec
      # release the finished routine's dev lock
      dev_locks[np.where(dev_cur_rtn_finish_time == t_min_exec)[0]] = 0
      dev_cur_rtn_finish_time[np.where(dev_cur_rtn_finish_time > 0)[0]] -= t_min_exec
    
    if (t_total > timeout and len(rtns) > 0):
      # print "Aborting {0} routines...".format(len(rtns))
      num_rtn_aborted += len(rtns)
      num_scheduled += n_total
      rtns = []

    # Deploy all runnable routines with the finished routines released
    for i, routine in enumerate(rtns):
      running_devs = set([idx for idx, dev_stat in enumerate(dev_locks) if dev_stat != 0])
      if len(running_devs.intersection(set(routine.dev_list))) == 0:  # The routine is able to run 
        # print "Routine {0} on dev {1} with running time {2} is scheduled at time {3}.".format(
          # routine.rid, routine.dev_list, sum(routine.dur_list), t_total)
        num_scheduled += 1
        running_devs |= set(routine.dev_list)
        running_idxs.append(i)
        # Set the lock for new running routine. lock finish time: routine finsh time
        dev_locks[routine.dev_list] = 1
        dev_cur_rtn_finish_time[routine.dev_list] = sum(routine.dur_list)

    # rtns stores the un-scheduled routiens
    rtns = [i for j, i in enumerate(rtns) if j not in running_idxs]
    running_idxs = []
    # print "Updated rtn size {0} with num_scheduled = {1} at time {2}".format(len(rtns), num_scheduled, t_total)
    # sleep(1)
    # If current time is > timeout and the rtn is not scheduled. The routines are aborted.

  return t_total, num_rtn_aborted * 1.0 /n_total


def timeRunAllStrategyNoAbort(rtns):
  return runSequentially(rtns), runWithCCLock(rtns), runWithDevLock(rtns), runSeqInsideRtnWithDevLock(rtns), runSeqInsideRtnWithCCLock(rtns)

def multiRunForSidORAbort(NUM_RUN, lrr_required = False, new_routine_bank=False, routine_bank=[]):
  t_sid_list = []
  t_sid_abt_list = []
  r_sid_abt_list = []
  t_sic_list = []
  t_sic_abt_list = []
  r_sic_abt_list = []

  if new_routine_bank:
    routine_bank = generateNRandomRtn(NUM_RTN)
  
  for i in xrange(NUM_RUN):

    print "RUN {0}".format(i)

    selected_rtns = pickConcurrentRtn(routine_bank, CONCURRENT_LEVEL, lrr_required)
    # for rtn in selected_rtns:
    #   rtn.print_rtn() 
    t_seqin_dev_lock = runSeqInsideRtnWithDevLock(selected_rtns)
    t_sid_abt, r_sid_abt = runSeqInsideRtnWithDevLockAndAbort(selected_rtns, ABORT_TIMEOUT)

    t_seqin_cc__lock = runSeqInsideRtnWithCCLock(selected_rtns)
    t_sic_abt, r_sic_abt = runSeqInsideRtnWithCCLockAndAbort(selected_rtns, ABORT_TIMEOUT)

    t_sid_list.append(t_seqin_dev_lock)
    t_sid_abt_list.append(t_sid_abt)
    r_sid_abt_list.append(r_sid_abt)

    t_sic_list.append(t_seqin_cc__lock)
    t_sic_abt_list.append(t_sic_abt)
    r_sic_abt_list.append(r_sic_abt)

  return t_sid_list, t_sid_abt_list, r_sid_abt_list, t_sic_list, t_sic_abt_list, r_sic_abt_list
  
def multiRunAllStrategies(NUM_RUN, lrr_required = False, new_routine_bank=False, routine_bank=[]):
  t_seq_list = []
  t_cc__list = []
  t_dev_list = []
  t_sid_list = []
  t_sic_list = []
  t_sid_abt_list = []
  r_sid_abt_list = []
  t_sic_abt_list = []
  r_sic_abt_list = []

  if new_routine_bank:
    routine_bank = generateNRandomRtn(NUM_RTN)

  print "The routine bank has {0} long running routines. ".format(countNumLRR(routine_bank))
  
  for i in xrange(NUM_RUN):

    print "RUN {0}".format(i)

    selected_rtns = pickConcurrentRtn(routine_bank, CONCURRENT_LEVEL, lrr_required)
    if lrr_required and countNumLRR(selected_rtns) > 1:
      print "The routine bank has {0} long running routines. ".format(countNumLRR(routine_bank))
    # for rtn in selected_rtns:
    #   rtn.print_rtn()

    t_sequential, t_cc_level_lock, t_dev_level_lock, t_seqin_dev_lock, t_seqin_cc_lock = timeRunAllStrategyNoAbort(selected_rtns)
    # print "Time -- Run sequentially: {0} \n\
    #       Run cc_lock: {1}\n\
    #       Run dev_lock: {2}".format(t_sequential, t_cc_level_lock, t_dev_level_lock)

    t_sid_abt, r_sid_abt = runSeqInsideRtnWithDevLockAndAbort(selected_rtns, ABORT_TIMEOUT)
    t_sic_abt, r_sic_abt = runSeqInsideRtnWithCCLockAndAbort(selected_rtns, ABORT_TIMEOUT)

    t_seq_list.append(t_sequential)
    t_cc__list.append(t_cc_level_lock)
    t_dev_list.append(t_dev_level_lock)
    t_sid_list.append(t_seqin_dev_lock)
    t_sic_list.append(t_seqin_cc_lock)
    t_sid_abt_list.append(t_sid_abt)
    r_sid_abt_list.append(r_sid_abt)
    t_sic_abt_list.append(t_sic_abt)
    r_sic_abt_list.append(r_sic_abt)

  # print "Sequential Avg: {0}\nCC_Lock Avg: {1}\nDev_Lock Avg:{2}\nSeq_in_Dev_Lock Avg:{3}\nAbt_Seq_in_Dev_lock Avg: {4} with abort rate {5}"\
  # .format(mean(t_seq_list), mean(t_cc__list), mean(t_dev_list), mean(t_sid_list), mean(t_sid_abt_list), mean(r_sid_abt_list))
  # print r_sid_abt_list
  return t_seq_list, t_cc__list, t_dev_list, t_sid_list, t_sic_list, t_sid_abt_list, r_sid_abt_list, t_sic_abt_list, r_sic_abt_list
  
def getNumLongRunningRun(lst):
  return len([i for i in lst if i > MAX_CMD_EXEC_TIME-1])

def countNumLRR(routine_bank):
  return len([1 for rtn in routine_bank if max(rtn.dur_list) > MAX_CMD_EXEC_TIME - 1])

if __name__ == "__main__":
  method_axis = range(1, 8)
  method_list = ['Sequential', 'CC_lock(par)', 'Dev_lock(par)', 'Dev_lock(seq)', 'CC_lock(seq)', 'Dev_lock(seq, abt)', 'CC_lock(seq, abt)']
  concurrent_level_list = range(2, 10)
  num_routine_list = range(6, 20, 2)
  num_device_list = range(8, 20, 2)
  max_cmd_num_per_rtn_list = range(3, 8)
  abort_timeout_list = [5, 10, 15, 20, 25]

  routine_bank = generateNRandomRtn(NUM_RTN)

  # routine_bank = generateCCVsDevRtn()
  # routine_bank = generateMultiAccessToOneDev()

  # for NUM_RTN in num_routine_list:
  # d_seq, d_cc, d_dev, d_sid, d_sic, d_abt, d_r_abt, d_cabt, d_r_cabt=  multiRunAllStrategies(NUM_RUN, LRR_REQUIRED, True)
  # print mean(d_r_abt), mean(d_r_cabt)
  # plt.hist(d_cc, normed=True, cumulative=True, label='CDF',
         # histtype='step', alpha=0.8, color='k')
  # d_seq_arr = np.array(d_dev).astype(float)
  # d_seq_arr /= d_seq_arr.sum()
  # Cd_seq = np.cumsum(d_seq)
  # plt.plot(range(NUM_RUN), d_seq_arr)
  # plt.plot(range(NUM_RUN), Cd_seq, 'r--')
  # plt.show()
  # mean_time_list.append([mean(d_seq), mean(d_cc), mean(d_dev), mean(d_sid), mean(d_sic), mean(d_abt)])

  # ###
  # ### PLOTTING total number of routines
  # ###
  # mean_time_list = []
  # x_axis = num_routine_list[:]
  # for NUM_RTN in num_routine_list:
  #   d_seq, d_cc, d_dev, d_sid, d_sic, d_abt, d_r_abt, d_cabt, d_r_cabt = multiRunAllStrategies(NUM_RUN, LRR_REQUIRED, True)
  #   mean_time_list.append([mean(d_seq), mean(d_cc), mean(d_dev), mean(d_sid), mean(d_sic), mean(d_abt)])
  
  # mean_time_matrix = np.array(mean_time_list)
  # plt.plot(x_axis, mean_time_matrix[:, 1], 'bs-', label=method_list[1])
  # plt.plot(x_axis, mean_time_matrix[:, 2], 'g^-', label=method_list[2])
  # plt.plot(x_axis, mean_time_matrix[:, 3], 'k2-', label=method_list[3])
  # plt.plot(x_axis, mean_time_matrix[:, 4], 'mo-', label=method_list[4])
  # plt.plot(x_axis, mean_time_matrix[:, 5], 'r+-', label=method_list[5])
  # plt.ylabel("Total execution time for all routines")
  # plt.xlabel("Total number of routines")
  # plt.show()


  # ###
  # ### PLOTTING concurrency level
  # ###

  mean_time_list = []
  std_time_list = []
  num_LR = []
  x_axis = concurrent_level_list[:]
  for CONCURRENT_LEVEL in concurrent_level_list:
    d_seq, d_cc, d_dev, d_sid, d_sic, d_abt, d_r_abt, d_cabt, d_r_cabt = multiRunAllStrategies(NUM_RUN, LRR_REQUIRED, False, routine_bank)
    mean_time_list.append([mean(d_seq), mean(d_cc), mean(d_dev), mean(d_sid), mean(d_sic), mean(d_abt), mean(d_r_abt), mean(d_cabt), mean(d_r_cabt)])
    std_time_list.append([stdev(d_seq), stdev(d_cc), stdev(d_dev), stdev(d_sid), stdev(d_abt), stdev(d_r_abt), stdev(d_cabt), stdev(d_r_cabt)])
    # num_LR.append([getNumLongRunningRun(lst) for lst in [d_seq, d_cc, d_dev, d_sid, d_sic, d_abt]])

  # print num_LR

  mean_time_matrix = np.array(mean_time_list)
  std_time_matrix = np.array(std_time_list)
  fig, ax1 = plt.subplots()
  ax2 = ax1.twinx()
  # plt.plot(x_axis, mean_time_matrix[:, 0], 'r1--', label=method_list[0])
  # ax1.errorbar(x_axis, mean_time_matrix[:, 1], yerr= std_time_matrix[:, 1], fmt='bs-', label=method_list[1])
  # ax1.errorbar(x_axis, mean_time_matrix[:, 2], yerr= std_time_matrix[:, 2], fmt='g^-', label=method_list[2])
  # ax1.errorbar(x_axis, mean_time_matrix[:, 3], yerr= std_time_matrix[:, 3], fmt='k2-', label=method_list[3])
  # ax1.errorbar(x_axis, mean_time_matrix[:, 4], yerr= std_time_matrix[:, 4], fmt='mo-', label=method_list[4])
  ax1.plot(x_axis, mean_time_matrix[:, 1], 'bs-', label=method_list[1])
  ax1.plot(x_axis, mean_time_matrix[:, 2], 'g^-', label=method_list[2])
  ax1.plot(x_axis, mean_time_matrix[:, 3], 'k2-', label=method_list[3])
  ax1.plot(x_axis, mean_time_matrix[:, 4], 'mo-', label=method_list[4])
  ax1.plot(x_axis, mean_time_matrix[:, 5], 'r+-', label=method_list[5])
  ax1.plot(x_axis, mean_time_matrix[:, 7], 'cp-', label=method_list[6])
  ax1.legend(loc="upper left")
  ax1.set_ylabel('Total execution time for all routines')
  ax2.plot(x_axis, mean_time_matrix[:, 6], 'y*--', label='Dev_lock Abort Rate')
  ax2.plot(x_axis, mean_time_matrix[:, 8], 'b*--', label='CC__lock Abort Rate')
  ax2.legend(loc="center left")
  ax2.set_ylabel('Abort rate (only for the last method)')
  ax1.set_xlabel('Number of concurrent routines')
  plt.show()


  ###
  ### PLOTTING total number of devices
  ###
  
  mean_time_list = []
  std_time_list = []
  x_axis = num_device_list[:]
  for TTL_NUM_DEV in num_device_list:
    d_seq, d_cc, d_dev, d_sid, d_sic, d_abt, d_r_abt, d_cabt, d_r_cabt = multiRunAllStrategies(NUM_RUN, LRR_REQUIRED, True)
    mean_time_list.append([mean(d_seq), mean(d_cc), mean(d_dev), mean(d_sid), mean(d_sic), mean(d_abt), mean(d_r_abt), mean(d_cabt), mean(d_r_cabt)])
    std_time_list.append([stdev(d_seq), stdev(d_cc), stdev(d_dev), stdev(d_sid), stdev(d_abt), stdev(d_r_abt), stdev(d_cabt), stdev(d_r_cabt)])
    
  mean_time_matrix = np.array(mean_time_list)
  std_time_matrix = np.array(std_time_list)
  fig, ax1 = plt.subplots()
  ax2 = ax1.twinx()
  ax1.plot(x_axis, mean_time_matrix[:, 1], 'bs-', label=method_list[1])
  ax1.plot(x_axis, mean_time_matrix[:, 2], 'g^-', label=method_list[2])
  ax1.plot(x_axis, mean_time_matrix[:, 3], 'k2-', label=method_list[3])
  ax1.plot(x_axis, mean_time_matrix[:, 4], 'mo-', label=method_list[4])
  ax1.plot(x_axis, mean_time_matrix[:, 5], 'r+-', label=method_list[5])
  ax1.plot(x_axis, mean_time_matrix[:, 7], 'cp-', label=method_list[6])
  ax1.legend(loc="upper left")
  ax1.set_ylabel('Total execution time for all routines')
  ax2.plot(x_axis, mean_time_matrix[:, 6], 'y*--', label='Dev_lock Abort Rate')
  ax2.plot(x_axis, mean_time_matrix[:, 8], 'b*--', label='CC__lock Abort Rate')
  ax2.legend(loc="center left")
  ax2.set_ylabel('Abort rate (only for the last method)')
  ax1.set_xlabel('Number of devices')
  plt.show()


  ###
  ### PLOTTING max number of command per routine
  ###
  
  mean_time_list = []
  std_time_list = []
  x_axis = max_cmd_num_per_rtn_list[:]
  for MAX_NUM_CMD_PER_RTN in max_cmd_num_per_rtn_list:
    d_seq, d_cc, d_dev, d_sid, d_sic, d_abt, d_r_abt, d_cabt, d_r_cabt = multiRunAllStrategies(NUM_RUN, LRR_REQUIRED, True)
    mean_time_list.append([mean(d_seq), mean(d_cc), mean(d_dev), mean(d_sid), mean(d_sic), mean(d_abt), mean(d_r_abt), mean(d_cabt), mean(d_r_cabt)])
    std_time_list.append([stdev(d_seq), stdev(d_cc), stdev(d_dev), stdev(d_sid), stdev(d_abt), stdev(d_r_abt), stdev(d_cabt), stdev(d_r_cabt)])
    
  mean_time_matrix = np.array(mean_time_list)
  std_time_matrix = np.array(std_time_list)
  fig, ax1 = plt.subplots()
  ax2 = ax1.twinx()
  ax1.plot(x_axis, mean_time_matrix[:, 1], 'bs-', label=method_list[1])
  ax1.plot(x_axis, mean_time_matrix[:, 2], 'g^-', label=method_list[2])
  ax1.plot(x_axis, mean_time_matrix[:, 3], 'k2-', label=method_list[3])
  ax1.plot(x_axis, mean_time_matrix[:, 4], 'mo-', label=method_list[4])
  ax1.plot(x_axis, mean_time_matrix[:, 5], 'r+-', label=method_list[5])
  ax1.plot(x_axis, mean_time_matrix[:, 7], 'cp-', label=method_list[6])
  ax1.legend(loc="upper left")
  ax1.set_ylabel('Total execution time for all routines')
  ax2.plot(x_axis, mean_time_matrix[:, 6], 'y*--', label='Dev_lock Abort Rate')
  ax2.plot(x_axis, mean_time_matrix[:, 8], 'b*--', label='CC__lock Abort Rate')
  ax2.legend(loc="center left")
  ax2.set_ylabel('Abort rate (only for the last method)')
  ax1.set_xlabel('Max number of command per routine')
  plt.show()


  ###
  ### PLOTTING different timeout
  ###
  
  mean_time_list = []
  std_time_list = []
  x_axis = abort_timeout_list[:]
  for ABORT_TIMEOUT in abort_timeout_list:
    d_sid, d_abt, d_r_abt, d_sic, d_cabt, d_r_cabt = multiRunForSidORAbort(NUM_RUN, LRR_REQUIRED, False, routine_bank)
    mean_time_list.append([mean(d_sid), mean(d_sic), mean(d_abt), mean(d_r_abt), mean(d_cabt), mean(d_r_cabt)])
    std_time_list.append([stdev(d_sid), stdev(d_sic), stdev(d_abt), stdev(d_r_abt), stdev(d_cabt), stdev(d_r_cabt)])

  mean_time_matrix = np.array(mean_time_list)
  std_time_matrix = np.array(std_time_list)
  fig, ax1 = plt.subplots()
  ax2 = ax1.twinx()
  # ax1.plot(x_axis, mean_time_matrix[:, 1], 'bs-', label=method_list[1])
  # ax1.plot(x_axis, mean_time_matrix[:, 2], 'g^-', label=method_list[2])
  ax1.plot(x_axis, mean_time_matrix[:, 0], 'k2-', label=method_list[3])
  ax1.plot(x_axis, mean_time_matrix[:, 1], 'mo-', label=method_list[4])
  ax1.plot(x_axis, mean_time_matrix[:, 2], 'r+-', label=method_list[5])
  ax1.plot(x_axis, mean_time_matrix[:, 4], 'cp-', label=method_list[6])
  ax1.legend(loc="upper left")
  ax1.set_ylabel('Total execution time for all routines')
  ax2.plot(x_axis, mean_time_matrix[:, 3], 'y*--', label='Dev_lock Abort Rate')
  ax2.plot(x_axis, mean_time_matrix[:, 5], 'b*--', label='CC__lock Abort Rate')
  ax2.legend(loc="center left")
  ax2.set_ylabel('Abort rate (only for the last method)')
  ax1.set_xlabel('Abort Timeout')
  plt.show()



  # Total execution time
  # Another strategy: 
  #   1. just try, if there is lock, just abort it. (maybe with a timeout) --> abort rate. (can be applied to multiple strategies)
  #   2. inside routine: sequential, across routine: parallel
  # Parameters at the head (+ execution time/lock time)

  # Trim down to nessesary variables.

  # Try different duration distribution with same E(X)