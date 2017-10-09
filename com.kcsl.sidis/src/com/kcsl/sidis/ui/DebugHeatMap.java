package com.kcsl.sidis.ui;

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;

import com.kcsl.sidis.dis.HeatMap;

public class DebugHeatMap extends ViewPart {

	public static final String ID = "com.kcsl.sidis.ui.DebugHeatMap"; //$NON-NLS-1$

	public DebugHeatMap() {
		setPartName("Debug Heat Map");
		setTitleImage(ResourceManager.getPluginImage("com.kcsl.sidis", "icons/toolbox.gif"));
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		composite.setLayout(new GridLayout(2, false));
		
		Label monochromeLabel = new Label(composite, SWT.NONE);
		monochromeLabel.setText("Monochrome: ");
		
		Composite monochromeColor = new Composite(composite, SWT.NONE);
		GridData gd_monochromeColor = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_monochromeColor.heightHint = 20;
		monochromeColor.setLayoutData(gd_monochromeColor);
		monochromeColor.setSize(584, 64);
		monochromeColor.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		
		Label invertedMonochromeLabel = new Label(composite, SWT.NONE);
		invertedMonochromeLabel.setText("Inverted Monochrome: ");
		
		Composite invertedMonochromeColor = new Composite(composite, SWT.NONE);
		GridData gd_invertedMonochromeColor = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_invertedMonochromeColor.heightHint = 20;
		invertedMonochromeColor.setLayoutData(gd_invertedMonochromeColor);
		invertedMonochromeColor.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		
		Label twoColorLabel = new Label(composite, SWT.NONE);
		twoColorLabel.setText("2 Color (Blue → Red)");
		
		Composite twoColor = new Composite(composite, SWT.NONE);
		GridData gd_twoColor = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_twoColor.heightHint = 20;
		twoColor.setLayoutData(gd_twoColor);
		twoColor.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		new Label(composite, SWT.NONE);
		
		Label percentageLabel = new Label(composite, SWT.NONE);
		percentageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		percentageLabel.setSize(584, 14);
		percentageLabel.setText("Percentage: 0");
		new Label(composite, SWT.NONE);
		
		Composite controlComposite = new Composite(composite, SWT.NONE);
		controlComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		controlComposite.setSize(584, 25);
		controlComposite.setLayout(new GridLayout(3, false));
		
		Label lowerBoundLabel = new Label(controlComposite, SWT.NONE);
		lowerBoundLabel.setText("0");
		
		Scale scale = new Scale(controlComposite, SWT.NONE);
		scale.setPageIncrement(1);
		scale.setIncrement(0);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label upperBoundLabel = new Label(controlComposite, SWT.NONE);
		upperBoundLabel.setText("100");
		
		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int lowerBound = Integer.parseInt(lowerBoundLabel.getText());
				int upperBound = Integer.parseInt(upperBoundLabel.getText());
				int value = scale.getSelection();
				double intensity = HeatMap.normalizeIntensity(value, lowerBound, upperBound);
				DecimalFormat format = new DecimalFormat("#.##");
				percentageLabel.setText("Intensity: " + format.format(intensity));
				
				java.awt.Color monochromeColorResult = HeatMap.getMonochromeHeatMapColor(intensity);
				monochromeColor.setBackground(new Color(parent.getDisplay(), monochromeColorResult.getRed(), monochromeColorResult.getGreen(), monochromeColorResult.getBlue()));
				
				java.awt.Color invertedMonochromeColorResult = HeatMap.getInvertedMonochromeHeatMapColor(intensity);
				invertedMonochromeColor.setBackground(new Color(parent.getDisplay(), invertedMonochromeColorResult.getRed(), invertedMonochromeColorResult.getGreen(), invertedMonochromeColorResult.getBlue()));
				
				java.awt.Color twoColorResult = HeatMap.getBlueRedGradientHeatMapColor(intensity);
				twoColor.setBackground(new Color(parent.getDisplay(), twoColorResult.getRed(), twoColorResult.getGreen(), twoColorResult.getBlue()));
			}
		});
	}

	@Override
	public void setFocus() {}
}
