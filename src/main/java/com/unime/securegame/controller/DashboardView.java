package com.unime.securegame.controller;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        setMaxWidth("960px");
        setWidthFull();
        setMargin(true);
        setSpacing(true);

        add(new H1("SecureGame — Gamified MFA Trainer"));
        add(new Paragraph("Practice authentication, phishing recognition, and security incident response."));
        add(new H2("Learning modules"));
        add(new Anchor("/index.html", "Open the interactive game and campaign"));
        add(new Anchor("/api/scenarios", "Browse saved authentication scenarios"));
        add(new Anchor("/api/risk/dataset/csv?samples=100&seed=42", "Download a reproducible sample dataset (CSV)"));
        add(new Paragraph("The interactive arcade remains the existing static game UI; this Vaadin route provides the required Flow application entry point."));
    }
}
