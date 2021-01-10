#!/usr/bin/python3

import sys
import random
import math
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
from datetime import datetime


#ELEMENTAL_RATIOS = [("Fire",1.6),("Air",5.93),("Water",16.33),("Earth",51.0)]

class Cell:
    PHI = (1 + 5 ** 0.5) / 2
    aether = float(random.uniform(0,10)) #stationary
    nether = float(random.uniform(0,10)) #aviary
    target_ratio = PHI

    def __init__(self):
        random.seed(datetime.now())
        self.aether = float(random.uniform(0,10))
        self.nether = float(random.uniform(0,10))

    def ratio(self):
        return float(self.aether / self.nether)

    def phi_delta(self):
        return float(self.ratio()) - float(self.target_ratio)

    def printMe(self):
        #print( "\rAe: %.2f; Ne: %.2f; ratio: %.4f " % (self.aether, self.nether, self.ratio()) )
        print( "%.2f; %.2f; %.4f " % (self.aether, self.nether, self.ratio()) )

    def step(self, nether_dynamic):
        tmp_aether = self.aether
        self.aether = self.nether * (self.target_ratio + (nether_dynamic*self.phi_delta()))
        self.nether = 1.0 / (tmp_aether * (self.target_ratio + ((1.0 - nether_dynamic)*self.phi_delta())))

# Arguments #
# [0] the file name for whatever reason
# [1] target ratio
# [2] Nether dynamic

cell = []
ae_plot = []
ne_plot = []
ratio_plot = []

nether_dynamic = 0.6

fig, axs = plt.subplots(6, 3) #rows, cols
nether_dynamic_slider = Slider(plt.axes([0.25, 0.15, 0.65, 0.03]), "nether dynamic", 0,1, valinit=nether_dynamic)
plt.subplots_adjust(left=0.1,bottom=0.35)

def init():
    global cell
    global ae_plot
    global ne_plot
    global ratio_plot
    cell = []
    ae_plot = []
    ne_plot = []
    ratio_plot = []
    for x in [0,1,2]:
        cell.append([])
        ae_plot.append([])
        ne_plot.append([])
        ratio_plot.append([])
        for y in [0,1,2]:
            cell[x].append(Cell())
            ae_plot[x].append([])
            ne_plot[x].append([])
            ratio_plot[x].append([])

def re_calculate_cell(x,y):
    global cell
    global ae_plot
    global ne_plot
    global ratio_plot
    while (not math.isclose(cell[x][y].ratio(),cell[x][y].target_ratio, abs_tol=1e-4)):
        ae_plot[x][y].append(cell[x][y].aether)
        ne_plot[x][y].append(cell[x][y].nether)
        ratio_plot[x][y].append(cell[x][y].ratio())
        cell[x][y].step(nether_dynamic)

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
            axs[y*2 + 1][x].text(0, 0, "(%i,%i)" % (x,y) , ha='center', va='center', size=12, alpha=.5)
            axs[y*2 + 1][x].plot(ratio_plot[x][y])
    fig.canvas.draw()
    fig.canvas.flush_events()

nether_dynamic_slider.on_changed(update)

if __name__ == '__main__':
    main()
