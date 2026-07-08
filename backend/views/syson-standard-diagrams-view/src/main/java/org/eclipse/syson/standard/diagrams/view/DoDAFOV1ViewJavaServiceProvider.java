package org.eclipse.syson.standard.diagrams.view;

import java.util.List;

import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.emf.IJavaServiceProvider;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFOV1ViewCreateService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DoDAFOV1ViewJavaServiceProvider implements IJavaServiceProvider {

    @Override
    public List<Class<?>> getServiceClasses(View view) {
        var descriptions = view.getDescriptions();
        var optDesc = descriptions.stream()
                .filter(desc -> DoDAFOV1ViewDiagramDescriptionProvider.DESCRIPTION_NAME.equals(desc.getName()))
                .findFirst();
        if (optDesc.isPresent()) {
            return List.of(DoDAFOV1ViewCreateService.class);
        }
        return List.of();
    }
}
