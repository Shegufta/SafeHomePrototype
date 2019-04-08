import random
import numpy as np
from statistics import mean

NUM_RUN = 10000
TTL_NUM_DEV = 10 # total number of device
MAX_NUM_CMD_PER_RTN = 6
NUM_RTN = 10         # total number of routines

CONCURRENT_LEVEL = 3 # run two concurrent routines together

class Routine:
  def __init__(self, dev_list, sta_list, dur_list):
    self.dev_list = dev_list
    self.sta_list = sta_list
    self.dur_list = dur_list

  def print_rtn(self):
    print "\nRoutine:"
    for i in xrange(len(self.dev_list)):
      print "    Dev: {0} with target state: {1} at time {2}.".format(self.dev_list[i], self.sta_list[i], self.dur_list[i])

def getOneRandomRtn(num_cmd):
  dev_list = random.sample(range(0, TTL_NUM_DEV), num_cmd)
  sta_list = [0] * num_cmd
  dur_list = [10] * num_cmd
  return Routine(dev_list, sta_list, dur_list)

def generateNRandomRtn(num_rtn):
  routine_bank = []
  # Generate routines
  for i in xrange(num_rtn):
    num_cmd = random.randint(2, MAX_NUM_CMD_PER_RTN)
    routine_bank.append(getOneRandomRtn(num_cmd))
    # routine_bank[i].print_rtn()

  return routine_bank

def generateCCVsDevRtn():
  routine_bank = []
  sta_list = [0] * 3
  dur_list = [10] * 3
  routine_bank.append(Routine([1, 2, 3], sta_list, dur_list))
  routine_bank.append(Routine([1, 4, 5], sta_list, dur_list))
  routine_bank.append(Routine([2, 4, 6], sta_list, dur_list))
  return routine_bank


def pickConcurrentRtn(rtn_bank, num_picked):
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

def timeRunAllStrategy(rtns):
  return runSequentially(rtns), runWithCCLock(rtns), runWithDevLock(rtns)

  
def main():
  t_seq_list = []
  t_cc__list = []
  t_dev_list = []
  routine_bank = generateNRandomRtn(NUM_RTN)
  # routine_bank = generateCCVsDevRtn()
  
  for i in xrange(NUM_RUN):

    selected_rtns = pickConcurrentRtn(routine_bank, CONCURRENT_LEVEL)
    # for rtn in selected_rtns:
    #   rtn.print_rtn()

    t_sequential, t_cc_level_lock, t_dev_level_lock = timeRunAllStrategy(selected_rtns)
    # print "Time -- Run sequentially: {0} \n\
    #       Run cc_lock: {1}\n\
    #       Run dev_lock: {2}".format(t_sequential, t_cc_level_lock, t_dev_level_lock)

    t_seq_list.append(t_sequential)
    t_cc__list.append(t_cc_level_lock)
    t_dev_list.append(t_dev_level_lock)

  print "Sequential Avg: {0}\nCC_Lock Avg: {1}\nDev_Lock Avg:{2}".format(mean(t_seq_list), mean(t_cc__list), mean(t_dev_list))
  

if __name__ == "__main__":
  main()






