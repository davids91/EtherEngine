#!/usr/bin/python3

import sys
import random
import math
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.widgets import Slider
from datetime import datetime


class Cell:
    PHI = (1 + 5 ** 0.5) / 2
    #Elements: Fire, Air, Water, Earth
    ELEMENTAL_RATIOS = [(PHI),(PHI + 1/PHI),(PHI + 1/PHI + 1/(PHI ** 2)),(PHI + 1/PHI + 1/(PHI ** 2) + 1/(PHI ** 3))]

    aether = float(random.uniform(0,10)) #stationary
    nether = float(random.uniform(0,10)) #aviary
    target_ratio = PHI
    requested_aether = 0
    requested_nether = 0
    available_aether = float(random.uniform(0,10))
    available_nether = float(random.uniform(0,10))

    def __init__(self):
        random.seed(datetime.now())
        self.aether = float(random.uniform(0,10))
        self.nether = float(random.uniform(0,10))
        requested_aether = 0
        requested_nether = 0
        available_aether = float(random.uniform(0,10))
        available_nether = float(random.uniform(0,10))

    def ratio(self):
        return float(self.nether / max(self.aether,0.000000000000001))

    def phi_delta(self):
        return float(self.ratio()) - float(self.target_ratio)

    def printMe(self):
        print( "%.2f; %.2f; %.4f ==> %.4f " % (self.aether, self.nether, self.ratio(), self.target_ratio) )

    def get_target_ratio(self):
        if self.ratio() < self.ELEMENTAL_RATIOS[0]:
            return self.ELEMENTAL_RATIOS[0]
        elif self.ratio() < self.ELEMENTAL_RATIOS[1]:
            return self.ELEMENTAL_RATIOS[1]
        elif self.ratio() < self.ELEMENTAL_RATIOS[2]:
            return self.ELEMENTAL_RATIOS[2]
        elif self.ratio() < self.ELEMENTAL_RATIOS[3]:
            return self.ELEMENTAL_RATIOS[3]
        else:
            return self.ELEMENTAL_RATIOS[3]

    def get_ether_step(self, requested, available):
        if(0 < requested):
            if(requested < available):
                return requested
            else:
                return available
        else:
            return requested
    def get_target_aether(self):
        return (self.nether / (self.target_ratio + (nether_dynamic*self.phi_delta())))

    def get_target_nether(self):
        return self.aether * (self.target_ratio + (1.0 - nether_dynamic)*(self.phi_delta()))

    def step(self, nether_dynamic):
        #decide target ratio
        if(not math.isclose(self.target_ratio,self.ratio())):
            self.target_ratio = self.get_target_ratio()

            #calculate the requested ether
            requested_aether = self.get_target_aether() - self.aether
            requested_nether = self.get_target_nether() - self.nether

            #step in the direction of the target ratio
            tmp_ether = self.get_ether_step(requested_aether,requested_aether) * 0.7
            self.aether += tmp_ether
            self.available_aether -= tmp_ether
            tmp_ether = self.get_ether_step(requested_nether,requested_nether) * 0.7
            self.nether += tmp_ether
            self.available_nether -= tmp_ether

            #radiate excess ether back into the reserves
            requested_aether = self.get_target_aether() - self.aether
            requested_nether = self.get_target_nether() - self.nether
            self.aether -= requested_aether * 0.1
            self.available_aether += requested_aether * 0.1
            self.nether -= requested_nether * 0.1
            self.available_aether += requested_nether * 0.1

            #radiate out some of the reserves because of usage
            tmp_ether = self.get_ether_step(requested_aether,self.available_aether) * 0.05
            if( abs(self.available_aether - tmp_ether) < abs(self.available_aether) ):
                self.aether -= tmp_ether
                self.available_aether += tmp_ether * 0.9
            tmp_ether = self.get_ether_step(requested_nether,self.available_nether) * 0.05
            if( abs(self.available_nether - tmp_ether) < abs(self.available_nether) ):
                self.nether -= tmp_ether
                self.available_nether += tmp_ether * 0.9

# Arguments #
# [0] the file name for whatever reason
# [1] target ratio
# [2] Nether dynamic

cell = []
ae_plot = []
ne_plot = []
ae_res_plot = []
ne_res_plot = []
ratio_plot = []
target_ratio_plot = []

nether_dynamic = 0.6


fig, axs = plt.subplots(6, 3) #rows, cols
nether_dynamic_slider = Slider(plt.axes([0.25, 0.15, 0.65, 0.03]), "nether dynamic", 0,1, valinit=nether_dynamic)
plt.subplots_adjust(left=0.1,bottom=0.35)
ratio_y_ax = [-Cell.ELEMENTAL_RATIOS[3],-Cell.ELEMENTAL_RATIOS[2],-Cell.ELEMENTAL_RATIOS[1],-Cell.ELEMENTAL_RATIOS[0],0.0, Cell.ELEMENTAL_RATIOS[0],Cell.ELEMENTAL_RATIOS[1],Cell.ELEMENTAL_RATIOS[2],Cell.ELEMENTAL_RATIOS[3]]

def init():
    global cell
    global ae_plot
    global ne_plot
    global ae_res_plot
    global ne_res_plot
    global ratio_plot
    global target_ratio_plot
    cell = []
    ae_plot = []
    ne_plot = []
    ae_res_plot = []
    ne_res_plot = []
    ratio_plot = []
    target_ratio_plot = []
    for x in [0,1,2]:
        cell.append([])
        ae_plot.append([])
        ne_plot.append([])
        ae_res_plot.append([])
        ne_res_plot.append([])
        ratio_plot.append([])
        target_ratio_plot.append([])
        for y in [0,1,2]:
            cell[x].append(Cell())
            ae_plot[x].append([])
            ne_plot[x].append([])
            ae_res_plot[x].append([])
            ne_res_plot[x].append([])
            ratio_plot[x].append([])
            target_ratio_plot[x].append([])

def re_calculate_cell(x,y):
    global cell
    global ae_plot
    global ne_plot
    global ae_res_plot
    global ne_res_plot
    global ratio_plot
    global target_ratio_plot
    watchdog = 0
    #while (not math.isclose(cell[x][y].ratio(),cell[x][y].target_ratio, abs_tol=1e-4) and (watchdog < 500)):
    while(watchdog < 10):
        ae_plot[x][y].append(cell[x][y].aether)
        ne_plot[x][y].append(cell[x][y].nether)
        ae_res_plot[x][y].append(cell[x][y].available_aether)
        ne_res_plot[x][y].append(cell[x][y].available_nether)
        ratio_plot[x][y].append(cell[x][y].ratio())
        target_ratio_plot[x][y].append(cell[x][y].target_ratio)
        cell[x][y].step(nether_dynamic)
        watchdog += 1
        #print("iter[%i](%i,%i):" % (watchdog,x,y))
        #cell[x][y].printMe()
def re_calculate():
    init()
    for x in [0,1,2]:
        for y in [0,1,2]:
            re_calculate_cell(x,y)
    update_plots()

def update(val):
    global nether_dynamic
    nether_dynamic = nether_dynamic_slider.val
    re_calculate()

def main():
    re_calculate()
    plt.show()

def update_plots():
    global fig
    global axs
    for x in [0,1,2]:
        for y in [0,1,2]:
            axs[y*2 + 0][x].clear()
            axs[y*2 + 1][x].clear()
            axs[y*2 + 0][x].plot(ae_plot[x][y], label="Æther", color="blue")
            axs[y*2 + 0][x].plot(ne_plot[x][y], label = "Nether", color="red")
            axs[y*2 + 0][x].plot(ae_res_plot[x][y], label="Æther reserves", color="lightblue")
            axs[y*2 + 0][x].plot(ne_res_plot[x][y], label = "Nether reserves", color="lightcoral")
            axs[y*2 + 1][x].text(0, 0, "(%i,%i)" % (x,y) , ha='center', va='center', size=12, alpha=.5)
            axs[y*2 + 1][x].set_yticks(ratio_y_ax)
            axs[y*2 + 1][x].plot(ratio_plot[x][y], color="goldenrod")
            axs[y*2 + 1][x].plot(target_ratio_plot[x][y], color="darkgoldenrod")
    fig.canvas.draw()
    fig.canvas.flush_events()

nether_dynamic_slider.on_changed(update)

if __name__ == '__main__':
    main()
