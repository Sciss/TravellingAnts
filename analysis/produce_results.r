library(ggplot2)

ants <- read.csv("../travelling-ants.log", header = TRUE)

draw <- function(ants.sample) {
  p <- ggplot(data = ants.sample)

  p + geom_smooth(aes(iteration, iteration.mean.cost, color = "Mean per iteration")) +
    geom_path(aes(iteration, best.cost, color = "Cumulative best")) +
    scale_color_hue() +
    labs(x = "Iteration index", y = "Solution cost", color = "Legend")
}

png('travelling-ants-iterations.png', width = 960, height = 480)
draw(head(ants, 1000))
dev.off()
