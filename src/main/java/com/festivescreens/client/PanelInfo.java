package com.festivescreens.client;

/** Implemented onto every AbstractContainerScreen by a mixin, exposing where its panel is drawn. */
public interface PanelInfo {
    int festive$left();

    int festive$top();

    int festive$width();

    int festive$height();
}
